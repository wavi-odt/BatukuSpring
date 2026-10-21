package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.services.DiscordAccountService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/discord/account")
public class DiscordAccountController {

    private final DiscordAccountService  discordAccountService;
    private final JwtUserDetailsService  jwtUserDetailsService;

    public DiscordAccountController(DiscordAccountService discordAccountService,
                                    JwtUserDetailsService jwtUserDetailsService) {
        this.discordAccountService = discordAccountService;
        this.jwtUserDetailsService  = jwtUserDetailsService;
    }

    @GetMapping
    public ResponseEntity<?> getAccount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(discordAccountService.getAccount(user));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> unlinkAccount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("status", discordAccountService.unlinkAccount(user)));
    }

    @PostMapping("/link-token")
    public ResponseEntity<Map<String, String>> generateLinkToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) Object ignored) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        String token = discordAccountService.generateLinkToken(user);
        return ResponseEntity.ok(Map.of("token", token, "expiresInSeconds", "600"));
    }
}
