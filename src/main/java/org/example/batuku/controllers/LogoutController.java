package org.example.batuku.controllers;

import jakarta.servlet.http.HttpServletResponse;
import org.example.batuku.domain.User;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.services.RefreshTokenService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class LogoutController {

    private final JwtUserDetailsService jwtUserDetailsService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public LogoutController(JwtUserDetailsService jwtUserDetailsService,
                             UserRepository userRepository,
                             RefreshTokenService refreshTokenService) {
        this.jwtUserDetailsService = jwtUserDetailsService;
        this.userRepository        = userRepository;
        this.refreshTokenService   = refreshTokenService;
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetails userDetails,
                                     HttpServletResponse response) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        user.setTokenValidFrom(LocalDateTime.now());
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());

        ResponseCookie clear = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true).sameSite("None")
                .path("/api/auth").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, clear.toString());

        return ResponseEntity.ok().build();
    }
}
