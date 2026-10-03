package org.example.batuku.controllers;

import org.example.batuku.domain.Location;
import org.example.batuku.domain.User;
import org.example.batuku.dto.RegisterRequest;
import org.example.batuku.dto.UserResponse;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.LocationRepository;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.domain.LoginToken;
import org.example.batuku.services.AuthService;
import org.example.batuku.services.LoginTokenService;
import org.example.batuku.services.PendingArtistClaimService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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
    private final LocationRepository locationRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtUserDetailsService jwtUserDetailsService;
    private final LoginTokenService loginTokenService;
    private final PendingArtistClaimService pendingArtistClaimService;

    public AuthController(AuthService authService, UserRepository userRepository,
                          ArtistProfileRepository artistProfileRepository,
                          LocationRepository locationRepository,
                          JwtTokenUtil jwtTokenUtil, JwtUserDetailsService jwtUserDetailsService,
                          LoginTokenService loginTokenService,
                          PendingArtistClaimService pendingArtistClaimService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.locationRepository = locationRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtUserDetailsService = jwtUserDetailsService;
        this.loginTokenService = loginTokenService;
        this.pendingArtistClaimService = pendingArtistClaimService;
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
            AuthService.ConfirmResult result = authService.confirmRegistration(token);
            if (result.isArtistPending()) {
                // Email verificado mas User ainda não criado — devolve claimToken para submeter o claim
                return ResponseEntity.ok(Map.of("pendingClaim", true, "claimToken", result.claimToken()));
            }
            UserDetails ud = jwtUserDetailsService.loadUserByUsername(result.user().getEmail());
            String jwt = jwtTokenUtil.generateToken(ud);
            return ResponseEntity.ok(Map.of("token", jwt, "user", UserResponse.from(result.user())));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * POST /api/auth/artist-claim-submit (público — sem JWT)
     * Submete os documentos de verificação para um registo de artista pendente.
     * Usa o claimToken gerado após verificação de email.
     */
    @PostMapping(value = "/artist-claim-submit", consumes = "multipart/form-data")
    public ResponseEntity<?> artistClaimSubmit(
            @RequestParam String claimToken,
            @RequestParam String spotifyArtistId,
            @RequestParam("selfie") MultipartFile selfie,
            @RequestParam("idDocument") MultipartFile idDocument) {
        try {
            pendingArtistClaimService.submitClaim(claimToken, spotifyArtistId, selfie, idDocument);
            return ResponseEntity.ok(Map.of("status", "PENDING"));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * POST /api/auth/oauth2/init-artist-claim
     *
     * Converte um utilizador OAuth2 recém-criado num registo pendente de artista.
     * Apaga o User da tabela users e cria um PendingRegistration com claimToken.
     * O User só é criado na tabela users após o admin aprovar o claim.
     */
    @PostMapping("/oauth2/init-artist-claim")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> initArtistClaim(@AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        String claimToken = pendingArtistClaimService.initFromOAuth2(user);
        return ResponseEntity.ok(Map.of("claimToken", claimToken));
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

    @GetMapping("/magic")
    public ResponseEntity<?> magicLogin(@RequestParam String token) {
        try {
            LoginToken lt = loginTokenService.consume(token);
            UserDetails ud = jwtUserDetailsService.loadUserByUsername(lt.getUser().getEmail());
            String jwt = jwtTokenUtil.generateToken(ud);
            return ResponseEntity.ok(Map.of("token", jwt, "redirectPath", lt.getRedirectPath()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /** GET /api/auth/locations — lista pública de localizações canónicas para o formulário de registo. */
    @GetMapping("/locations")
    public ResponseEntity<List<Map<String, String>>> getLocations() {
        List<Map<String, String>> locations = locationRepository.findAll().stream()
                .map(l -> Map.of("value", l.getValue(), "group", l.getLocationGroup()))
                .toList();
        return ResponseEntity.ok(locations);
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
                    Long artistProfileId   = profile.map(p -> p.getId()).orElse(null);
                    String spotifyArtistId = profile.map(p -> p.getSpotifyArtistId()).orElse(null);
                    String artistImageUrl  = profile.map(p -> p.getImageUrl()).orElse(null);
                    return ResponseEntity.ok(UserResponse.from(user, artistProfileId, spotifyArtistId, artistImageUrl));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
