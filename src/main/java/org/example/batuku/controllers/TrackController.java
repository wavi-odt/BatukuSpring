package org.example.batuku.controllers;

import org.example.batuku.domain.User;
import org.example.batuku.dto.TrackResponse;
import org.example.batuku.services.TrackService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tracks")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class TrackController {

    private final TrackService trackService;
    private final JwtUserDetailsService jwtUserDetailsService;

    public TrackController(TrackService trackService,
                           JwtUserDetailsService jwtUserDetailsService) {
        this.trackService = trackService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<TrackResponse> create(@AuthenticationPrincipal UserDetails userDetails,
                                                @RequestParam String title,
                                                @RequestParam String genre,
                                                @RequestParam("audio") MultipartFile audio,
                                                @RequestParam(value = "cover", required = false) MultipartFile cover,
                                                @RequestParam(value = "scheduledAt", required = false) String scheduledAt) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        LocalDateTime scheduled = (scheduledAt != null && !scheduledAt.isBlank())
                ? LocalDateTime.parse(scheduledAt) : null;
        TrackResponse r = trackService.createFromUploadWithStats(user, title, genre, audio, cover, scheduled);
        return ResponseEntity.created(URI.create("/api/tracks/" + r.getId())).body(r);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<TrackResponse> update(@AuthenticationPrincipal UserDetails userDetails,
                                                @PathVariable Long id,
                                                @RequestBody Map<String, String> body) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        try {
            return ResponseEntity.ok(trackService.updateWithStats(id, body.get("title"), body.get("genre"), user));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Long id) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        try {
            trackService.delete(id, user);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/admin/backfill-durations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> backfillDurations() {
        return ResponseEntity.ok(trackService.backfillDurations());
    }

    @GetMapping
    public List<TrackResponse> listAll() {
        return trackService.listAllWithLikes();
    }

    @PatchMapping(value = "/{id}/cover", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<TrackResponse> updateCover(@AuthenticationPrincipal UserDetails userDetails,
                                                     @PathVariable Long id,
                                                     @RequestParam("cover") MultipartFile cover) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        try {
            return ResponseEntity.ok(trackService.updateCoverWithStats(id, user, cover));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/following")
    public ResponseEntity<List<TrackResponse>> getFollowingTracks(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = jwtUserDetailsService.loadUserEntity(userDetails.getUsername());
        return ResponseEntity.ok(trackService.getFollowingTracks(user));
    }

    @GetMapping("/artist/{artistProfileId}")
    public List<TrackResponse> listByArtist(@PathVariable Long artistProfileId) {
        return trackService.listByArtistWithStats(artistProfileId);
    }

    @GetMapping("/{id}")
    public TrackResponse getTrack(@PathVariable Long id) {
        return trackService.getTrackWithStats(id);
    }
}
