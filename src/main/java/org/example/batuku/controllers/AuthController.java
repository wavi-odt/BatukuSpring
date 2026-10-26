package org.example.batuku.controllers;

import org.example.batuku.dto.RegisterRequest;
import org.example.batuku.dto.UserResponse;
import org.example.batuku.services.AuthService;
import org.example.batuku.services.LoginTokenService;
import org.example.batuku.services.PendingArtistClaimService;
import org.example.batuku.domain.LoginToken;
import org.example.batuku.domain.User;
import org.example.batuku.utils.JwtTokenUtil;
import org.example.batuku.utils.JwtUserDetailsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class AuthController {

    private final AuthService              authService;
    private final JwtTokenUtil             jwtTokenUtil;
    private final JwtUserDetailsService    jwtUserDetailsService;
    private final LoginTokenService        loginTokenService;
    private final PendingArtistClaimService pendingArtistClaimService;

    public AuthController(AuthService authService,
                          JwtTokenUtil jwtTokenUtil,
                          JwtUserDetailsService jwtUserDetailsService,
                          LoginTokenService loginTokenService,
                          PendingArtistClaimService pendingArtistClaimService) {
        this.authService              = authService;
        this.jwtTokenUtil             = jwtTokenUtil;
        this.jwtUserDetailsService    = jwtUserDetailsService;
        this.loginTokenService        = loginTokenService;
        this.pendingArtistClaimService = pendingArtistClaimService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.initRegistration(request);
            return ResponseEntity.accepted()
                    .body(Map.of("message", "Email de verificação enviado para " + request.getEmail()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
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
                return ResponseEntity.ok(Map.of("pendingClaim", true, "claimToken", result.claimToken()));
            }
            UserDetails ud = jwtUserDetailsService.loadUserByUsername(result.user().getEmail());
            String jwt = jwtTokenUtil.generateToken(ud);
            return ResponseEntity.ok(Map.of("token", jwt, "user", UserResponse.from(result.user())));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

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

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email obrigatório."));
        }
        authService.forgotPassword(email);
        return ResponseEntity.ok(Map.of("message", "Se este email estiver registado, receberás um link de recuperação em breve."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token       = body.get("token");
        String newPassword = body.get("newPassword");
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token e nova password são obrigatórios."));
        }
        try {
            authService.resetPassword(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password alterada com sucesso."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/locations")
    public ResponseEntity<List<Map<String, String>>> getLocations() {
        return ResponseEntity.ok(authService.getLocations());
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Não autenticado"));
        }
        try {
            return ResponseEntity.ok(authService.getMe(userDetails.getUsername()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
