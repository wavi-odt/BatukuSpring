package org.example.batuku.controllers;

import org.example.batuku.services.DiscordBotService;
import org.example.batuku.services.DiscordEventService;
import org.example.batuku.services.DiscordNotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/discord")
public class DiscordWebhookController {

    private final DiscordEventService          discordEventService;
    private final DiscordNotificationService   discordNotificationService;
    private final DiscordBotService            discordBotService;

    @Value("${batuku.discord.webhook-secret:}")
    private String webhookSecret;

    public DiscordWebhookController(DiscordEventService discordEventService,
                                    DiscordNotificationService discordNotificationService,
                                    DiscordBotService discordBotService) {
        this.discordEventService        = discordEventService;
        this.discordNotificationService = discordNotificationService;
        this.discordBotService          = discordBotService;
    }

    record IncomingEvent(
        String discordUserId,
        String eventType,
        String externalReference,
        String metadataJson
    ) {}

    @PostMapping("/events")
    public ResponseEntity<Map<String, String>> receiveEvent(
            @RequestHeader(value = "X-Discord-Secret", required = false) String secret,
            @RequestBody IncomingEvent body) {

        if (webhookSecret.isBlank() || !webhookSecret.equals(secret)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        return ResponseEntity.ok(discordEventService.processEvent(
                body.discordUserId(), body.eventType(),
                body.externalReference(), body.metadataJson()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/test-webhook")
    public ResponseEntity<Map<String, String>> testWebhook() {
        discordNotificationService.notifyMilestone("🧪 Teste de webhook do Batuku, está a funcionar!");
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/bot-status")
    public ResponseEntity<Map<String, String>> botStatus() {
        return ResponseEntity.ok(Map.of("status", discordBotService.isOnline() ? "online" : "offline"));
    }
}
