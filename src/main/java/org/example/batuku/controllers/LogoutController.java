package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.services.RefreshTokenService;
import org.example.batuku.utils.JwtUserDetailsService;
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
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        user.setTokenValidFrom(LocalDateTime.now());
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
        return ResponseEntity.ok().build();
    }
}
