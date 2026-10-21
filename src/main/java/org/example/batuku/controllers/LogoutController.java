package org.example.batuku.controllers;

import jakarta.servlet.http.HttpServletResponse;
import org.example.batuku.services.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class LogoutController {

    private final AuthService authService;

    public LogoutController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetails userDetails,
                                    HttpServletResponse response) {
        if (userDetails != null) {
            authService.logout(userDetails.getUsername());
        }

        ResponseCookie clear = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true).sameSite("None")
                .path("/api/auth").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, clear.toString());

        return ResponseEntity.ok().build();
    }
}
