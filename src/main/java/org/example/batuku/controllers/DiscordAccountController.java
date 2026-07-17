package org.example.batuku.controllers;

import org.example.batuku.domain.DiscordAccount;
import org.example.batuku.domain.User;
import org.example.batuku.repository.DiscordAccountRepository;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/discord/account")
public class DiscordAccountController {

    private final DiscordAccountRepository discordAccountRepository;
    private final JwtUserDetailsService jwtUserDetailsService;

    public DiscordAccountController(DiscordAccountRepository discordAccountRepository,
                                    JwtUserDetailsService jwtUserDetailsService) {
        this.discordAccountRepository = discordAccountRepository;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @GetMapping
    public ResponseEntity<?> getAccount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return discordAccountRepository.findByUserId(user.getId())
                .<ResponseEntity<?>>map(acc -> ResponseEntity.ok(Map.of(
                    "discordUserId", acc.getDiscordUserId(),
                    "discordUsername", acc.getDiscordUsername(),
                    "linkedAt", acc.getLinkedAt().toString()
                )))
                .orElse(ResponseEntity.ok(Map.of("linked", false)));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> unlinkAccount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        Optional<DiscordAccount> account = discordAccountRepository.findByUserId(user.getId());
        if (account.isEmpty()) {
            return ResponseEntity.ok(Map.of("status", "not_linked"));
        }
        discordAccountRepository.delete(account.get());
        return ResponseEntity.ok(Map.of("status", "unlinked"));
    }

    @PostMapping("/link-token")
    public ResponseEntity<Map<String, String>> generateLinkToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) Object ignored) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        String token = DiscordLinkTokenStore.generate(user.getId());
        return ResponseEntity.ok(Map.of("token", token, "expiresInSeconds", "600"));
    }
}
