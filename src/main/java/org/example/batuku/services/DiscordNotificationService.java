package org.example.batuku.services;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class DiscordNotificationService {

    private static final Logger log = LoggerFactory.getLogger(DiscordNotificationService.class);
    private static final int COLOR_GREEN = 1948500;

    private final RestClient restClient;

    @Value("${batuku.discord.webhook-url:}")
    private String webhookUrl;

    @Value("${batuku.app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    public DiscordNotificationService(RestClient restClient) {
        this.restClient = restClient;
    }

    public void notifyTrackPublished(Track track, ArtistProfile artist) {
        if (webhookUrl.isBlank()) return;

        Map<String, Object> embed = Map.of(
            "title", "🎵 " + track.getTitle(),
            "description", "**" + artist.getName() + "** acabou de publicar uma nova faixa!",
            "color", COLOR_GREEN,
            "url", appBaseUrl + "/track/" + track.getId(),
            "fields", List.of(
                Map.of("name", "Artista", "value", artist.getName(), "inline", true),
                Map.of("name", "Título", "value", track.getTitle(), "inline", true)
            )
        );

        sendEmbed(embed);
    }

    public void notifyMilestone(String message) {
        if (webhookUrl.isBlank()) return;
        Map<String, Object> embed = Map.of(
            "title", "🏆 Conquista Batuku",
            "description", message,
            "color", COLOR_GREEN
        );
        sendEmbed(embed);
    }

    private void sendEmbed(Map<String, Object> embed) {
        try {
            Map<String, Object> payload = Map.of(
                "username", "Batuku",
                "embeds", List.of(embed)
            );
            restClient.post()
                .uri(webhookUrl)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Discord webhook failed: {}", e.getMessage());
        }
    }
}
