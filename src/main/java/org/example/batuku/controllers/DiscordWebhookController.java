package org.example.batuku.controllers;

import org.example.batuku.domain.DiscordAccount;
import org.example.batuku.domain.DiscordEvent;
import org.example.batuku.repository.DiscordAccountRepository;
import org.example.batuku.repository.DiscordEventRepository;
import org.example.batuku.services.DiscordBotService;
import org.example.batuku.services.DiscordNotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/discord")
public class DiscordWebhookController {

    private final DiscordAccountRepository discordAccountRepository;
    private final DiscordEventRepository discordEventRepository;
    private final DiscordNotificationService discordNotificationService;
    private final DiscordBotService discordBotService;

    @Value("${batuku.discord.webhook-secret:}")
    private String webhookSecret;

    public DiscordWebhookController(DiscordAccountRepository discordAccountRepository,
                                    DiscordEventRepository discordEventRepository,
                                    DiscordNotificationService discordNotificationService,
                                    DiscordBotService discordBotService) {
        this.discordAccountRepository = discordAccountRepository;
        this.discordEventRepository = discordEventRepository;
        this.discordNotificationService = discordNotificationService;
        this.discordBotService = discordBotService;
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

        DiscordEvent.EventType type;
        try {
            type = DiscordEvent.EventType.valueOf(body.eventType());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown eventType: " + body.eventType()));
        }

        DiscordAccount account = discordAccountRepository.findByDiscordUserId(body.discordUserId())
                .orElse(null);

        if (account == null) {
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "Discord account not linked"));
        }

        DiscordEvent event = new DiscordEvent();
        event.setDiscordAccount(account);
        event.setEventType(type);
        event.setExternalReference(body.externalReference());
        event.setMetadataJson(body.metadataJson());
        discordEventRepository.save(event);

        return ResponseEntity.ok(Map.of("status", "saved"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/test-webhook")
    public ResponseEntity<Map<String, String>> testWebhook() {
        discordNotificationService.notifyMilestone("🧪 Teste de webhook do Batuku — está a funcionar!");
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/bot-status")
    public ResponseEntity<Map<String, String>> botStatus() {
        return ResponseEntity.ok(Map.of("status", discordBotService.isOnline() ? "online" : "offline"));
    }
}
