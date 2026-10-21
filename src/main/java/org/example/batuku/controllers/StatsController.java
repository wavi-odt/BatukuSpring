package org.example.batuku.controllers;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.ForecastResponse;
import org.example.batuku.dto.StatsResponse;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.services.StatsService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class StatsController {

    private final StatsService           statsService;
    private final TrackRepository        trackRepository;
    private final JwtUserDetailsService  jwtUserDetailsService;

    public StatsController(StatsService statsService,
                           TrackRepository trackRepository,
                           JwtUserDetailsService jwtUserDetailsService) {
        this.statsService          = statsService;
        this.trackRepository       = trackRepository;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @GetMapping("/api/stats/me")
    public ResponseEntity<StatsResponse> getMyStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "30d") String period) {

        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        if (statsService.findProfileByUserId(user.getId()).isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        ArtistProfile profile = statsService.findProfileByUserId(user.getId()).get();
        return ResponseEntity.ok(statsService.getStats(profile.getId(), period));
    }

    @GetMapping("/api/stats/weekly-summary")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<Map<String, String>> getWeeklySummary(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        if (statsService.findProfileByUserId(user.getId()).isEmpty())
            return ResponseEntity.noContent().build();

        ArtistProfile profile = statsService.findProfileByUserId(user.getId()).get();
        String summary = statsService.getWeeklySummary(profile);
        return ResponseEntity.ok(Map.of("summary", summary));
    }

    @GetMapping("/api/stats/me/forecast")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<ForecastResponse> getForecast(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "30") int historyDays,
            @RequestParam(defaultValue = "7")  int forecastDays) {

        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        if (statsService.findProfileByUserId(user.getId()).isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        ArtistProfile profile = statsService.findProfileByUserId(user.getId()).get();
        return ResponseEntity.ok(statsService.getForecast(profile.getId(), historyDays, forecastDays));
    }

    @PostMapping("/api/tracks/{id}/play")
    public ResponseEntity<Map<String, Long>> recordPlay(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Track track = trackRepository.findById(id).orElse(null);
            User user = null;
            if (userDetails != null) {
                try { user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername()); } catch (Exception ignored) {}
            }
            String country = body != null ? body.get("country") : null;
            String context = body != null ? body.get("context") : null;
            return statsService.recordPlay(track, country, context, user)
                    .map(playId -> ResponseEntity.ok(Map.of("playId", playId)))
                    .orElse(ResponseEntity.ok(Map.of()));
        } catch (Exception ignored) {}
        return ResponseEntity.ok(Map.of());
    }

    @PatchMapping("/api/plays/{id}")
    public ResponseEntity<Void> updatePlay(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            Integer durationPlayed = body.get("durationPlayed") instanceof Number n ? n.intValue() : null;
            Boolean isFullPlay     = body.get("isFullPlay") instanceof Boolean b ? b : null;
            statsService.updatePlay(id, durationPlayed, isFullPlay);
        } catch (Exception ignored) {}
        return ResponseEntity.noContent().build();
    }
}
