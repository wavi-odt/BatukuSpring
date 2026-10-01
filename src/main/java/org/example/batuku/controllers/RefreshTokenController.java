package org.example.batuku.controllers;

import org.example.batuku.services.RefreshTokenService;
import org.example.batuku.services.RefreshTokenService.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    public RefreshTokenController(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    public record RefreshRequest(String refreshToken) {}
    public record RefreshResponse(String token, String refreshToken) {}

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            RotationResult result = refreshTokenService.rotate(request.refreshToken());
            return ResponseEntity.ok(new RefreshResponse(result.newAccessToken(), result.newRefreshToken()));
        } catch (TokenReuseDetectedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "TOKEN_REUSE_DETECTED"));
        } catch (InvalidRefreshTokenException | RefreshTokenExpiredException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "INVALID_TOKEN"));
        }
    }
}
