package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.RegisterRequest;
import org.example.batuku.dto.UserResponse;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.services.AuthService;
import org.example.batuku.utils.JwtTokenUtil;
import org.example.batuku.utils.JwtUserDetailsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller de autenticação do Batuku.
 *
 * Endpoints:
 *   POST /api/auth/register  → criar conta nova
 *   GET  /api/auth/me        → dados do utilizador autenticado
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtUserDetailsService jwtUserDetailsService;

    public AuthController(AuthService authService, UserRepository userRepository,
                          ArtistProfileRepository artistProfileRepository,
                          JwtTokenUtil jwtTokenUtil, JwtUserDetailsService jwtUserDetailsService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    /**
     * POST /api/auth/register
     *
     * Cria uma conta nova. Não requer autenticação (está em permitAll).
     *
     * Body exemplo:
     * {
     *   "email": "joao@exemplo.com",
     *   "username": "joao123",
     *   "password": "minha_pass",
     *   "name": "João Silva",
     *   "country": "PT"   (opcional)
     * }
     *
     * Resposta 201 Created:
     * { "id": 1, "email": "...", "name": "...", "userRole": "FAN", ... }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.initRegistration(request);
            return ResponseEntity.accepted()
                    .body(Map.of("message", "Email de verificação enviado para " + request.getEmail()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        try {
            authService.resendVerification(body.get("email"));
            return ResponseEntity.ok(Map.of("message", "Email de verificação reenviado."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            User created = authService.confirmRegistration(token);
            if (!created.isEnabled()) {
                UserDetails ud = jwtUserDetailsService.loadUserByUsername(created.getEmail());
                String jwt = jwtTokenUtil.generateToken(ud);
                return ResponseEntity.ok(
                        Map.of("pendingValidation", true, "email", created.getEmail(), "token", jwt));
            }
            UserDetails ud = jwtUserDetailsService.loadUserByUsername(created.getEmail());
            String jwt = jwtTokenUtil.generateToken(ud);
            return ResponseEntity.ok(Map.of("token", jwt, "user", UserResponse.from(created)));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * POST /api/auth/oauth2/upgrade-to-artist
     *
     * Converte uma conta FAN (criada via OAuth2) para ARTIST.
     * Necessário quando o utilizador iniciou o registo OAuth com intenção de artista.
     * Cria o ArtistProfile, coloca enabled=false e devolve um novo JWT com ROLE_ARTIST.
     */
    @PostMapping("/oauth2/upgrade-to-artist")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> upgradeToArtist(@AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        authService.upgradeToArtist(user);
        UserDetails updated = jwtUserDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtTokenUtil.generateToken(updated);
        return ResponseEntity.ok(Map.of("pendingValidation", true, "token", token, "email", user.getEmail()));
    }

    @PostMapping("/welcome")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> sendWelcome(@AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        if (user.getUserRole() == User.UserRole.FAN && user.isEnabled()) {
            try { authService.sendWelcomeEmail(user); } catch (Exception ignored) {}
        }
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/auth/me
     *
     * Devolve os dados do utilizador que fez o pedido.
     * Requer Bearer token no header Authorization.
     *
     * O Spring Security já validou o JWT no JwtRequestFilter.
     * Aqui apenas lemos o email do SecurityContext e buscamos o utilizador.
     */
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Não autenticado"));
        }

        String email = auth.getName(); // o nome é o email (definido no JwtUserDetailsService)

        return userRepository.findByEmail(email)
                .map(user -> {
                    var profile = artistProfileRepository.findByUserId(user.getId());
                    Long artistProfileId  = profile.map(p -> p.getId()).orElse(null);
                    String spotifyArtistId = profile.map(p -> p.getSpotifyArtistId()).orElse(null);
                    return ResponseEntity.ok(UserResponse.from(user, artistProfileId, spotifyArtistId));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
