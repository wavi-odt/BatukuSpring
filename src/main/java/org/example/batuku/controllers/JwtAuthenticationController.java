package org.example.batuku.controllers;

import jakarta.servlet.http.HttpServletResponse;
import org.example.batuku.domain.User;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.services.RefreshTokenService;
import org.example.batuku.utils.JwtRequest;
import org.example.batuku.utils.JwtResponse;
import org.example.batuku.utils.JwtTokenUtil;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;

import java.util.Map;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

// ─── FICHEIRO DO PROFESSOR, NAO ALTERAR ────────────────────────────
// Endpoint: POST /authenticate
// Body: { "username": "email@exemplo.com", "password": "..." }
// Resposta: { "token": "eyJ...", "refreshToken": "..." }
@RestController
@CrossOrigin
public class JwtAuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JwtUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest,
                                                        HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getUsername(), authenticationRequest.getPassword()));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "PENDING_VALIDATION"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "INVALID_CREDENTIALS"));
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);

        User user = userRepository.findByEmailOrUsername(authenticationRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
        String refreshTokenRaw = refreshTokenService.issue(user);

        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshTokenRaw).toString());

        return ResponseEntity.ok(new JwtResponse(token));
    }

    private static ResponseCookie buildRefreshCookie(String value) {
        return ResponseCookie.from("refreshToken", value)
                .httpOnly(true).secure(true).sameSite("None")
                .path("/api/auth").maxAge(7 * 24 * 60 * 60L).build();
    }
}
