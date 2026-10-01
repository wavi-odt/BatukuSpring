package org.example.batuku.controllers;

import jakarta.servlet.http.HttpServletResponse;
import org.example.batuku.services.RefreshTokenService;
import org.example.batuku.services.RefreshTokenService.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "MISSING_TOKEN"));
        }

        try {
            RotationResult result = refreshTokenService.rotate(refreshToken);
            response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.newRefreshToken()).toString());
            return ResponseEntity.ok(Map.of("token", result.newAccessToken()));
        } catch (TokenReuseDetectedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "TOKEN_REUSE_DETECTED"));
        } catch (InvalidRefreshTokenException | RefreshTokenExpiredException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "INVALID_TOKEN"));
        }
    }

    private static ResponseCookie buildRefreshCookie(String value) {
        return ResponseCookie.from("refreshToken", value)
                .httpOnly(true).secure(true).sameSite("None")
                .path("/api/auth").maxAge(7 * 24 * 60 * 60L).build();
    }
}
