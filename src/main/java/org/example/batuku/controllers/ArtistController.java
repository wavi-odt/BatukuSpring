package org.example.batuku.controllers;

import jakarta.validation.Valid;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.dto.*;
import org.example.batuku.services.ArtistProfileService;
import org.example.batuku.utils.JwtUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/artists")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class ArtistController {

    private final ArtistProfileService artistProfileService;
    private final JwtUserDetailsService jwtUserDetailsService;

    public ArtistController(ArtistProfileService artistProfileService,
                            JwtUserDetailsService jwtUserDetailsService) {
        this.artistProfileService = artistProfileService;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @GetMapping("/{id}")
    public ArtistDetailResponse getArtist(@PathVariable Long id) {
        return artistProfileService.getArtistDetail(id);
    }

    @GetMapping("/options")
    public ArtistOptionsResponse getOptions() {
        return artistProfileService.getOptions();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Não autenticado ou sem perfil de artista."));
        ArtistProfile profile = artistProfileService.findByEmail(userDetails.getUsername()).orElse(null);
        if (profile == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Não autenticado ou sem perfil de artista."));
        return ResponseEntity.ok(ArtistMeResponse.from(profile));
    }

    @PutMapping("/me/name")
    public ResponseEntity<?> updateName(@RequestBody Map<String, String> body,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        ArtistProfile profile = artistProfileService.findByEmail(userDetails.getUsername()).orElse(null);
        if (profile == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String savedName = artistProfileService.updateName(profile, body.getOrDefault("name", "").trim());
        return ResponseEntity.ok(Map.of("name", savedName));
    }

    @PutMapping("/me/bio")
    public ResponseEntity<?> updateBio(@Valid @RequestBody ArtistBioRequest request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        ArtistProfile profile = artistProfileService.findByEmail(userDetails.getUsername()).orElse(null);
        if (profile == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        artistProfileService.updateBio(profile, request.getBio());
        return ResponseEntity.ok(Map.of("message", "Bio atualizada."));
    }

    @PutMapping("/me/location")
    public ResponseEntity<?> updateLocation(@Valid @RequestBody ArtistLocationRequest request,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        ArtistProfile profile = artistProfileService.findByEmail(userDetails.getUsername()).orElse(null);
        if (profile == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        artistProfileService.updateLocation(profile, request.getLocation());
        return ResponseEntity.ok(Map.of("message", "Localização atualizada."));
    }

    @PutMapping("/me/genres")
    public ResponseEntity<?> updateGenres(@Valid @RequestBody ArtistGenresRequest request,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        ArtistProfile profile = artistProfileService.findByEmail(userDetails.getUsername()).orElse(null);
        if (profile == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        artistProfileService.updateGenres(profile, request.getGenres());
        return ResponseEntity.ok(Map.of("message", "Géneros atualizados."));
    }

    @GetMapping("/me/links")
    public ResponseEntity<?> getMyLinks(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        ArtistProfile profile = artistProfileService.findByEmail(userDetails.getUsername()).orElse(null);
        if (profile == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<Map<String, String>> result = profile.getLinks() != null
                ? profile.getLinks().stream()
                        .filter(l -> l.getKind() != null && !l.getKind().isBlank())
                        .map(l -> Map.of("kind", l.getKind(), "handle", l.getHandle() != null ? l.getHandle() : ""))
                        .toList()
                : List.of();
        return ResponseEntity.ok(result);
    }

    @PutMapping("/me/links")
    public ResponseEntity<?> updateLinks(@RequestBody Map<String, List<Map<String, String>>> body,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        ArtistProfile profile = artistProfileService.findByEmail(userDetails.getUsername()).orElse(null);
        if (profile == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        artistProfileService.updateLinks(profile, body.getOrDefault("social", List.of()));
        return ResponseEntity.ok(Map.of("message", "Links atualizados."));
    }

    @PutMapping("/me/languages")
    public ResponseEntity<?> updateLanguages(@Valid @RequestBody ArtistLanguagesRequest request,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        ArtistProfile profile = artistProfileService.findByEmail(userDetails.getUsername()).orElse(null);
        if (profile == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        artistProfileService.updateLanguages(profile, request.getLanguages());
        return ResponseEntity.ok(Map.of("message", "Línguas atualizadas."));
    }

    @GetMapping("/{id}/top-tracks")
    public ResponseEntity<List<SpotifyTrackResponse>> getTopTracks(
            @PathVariable Long id,
            @RequestParam(defaultValue = "PT") String market) {
        return ResponseEntity.ok(artistProfileService.getTopTracks(id, market));
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<SimilarArtistResponse>> getSimilar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(artistProfileService.getSimilarArtists(id, limit));
    }

    @GetMapping("/suggested")
    public ResponseEntity<List<ArtistFollowResponse>> getSuggested(
            @AuthenticationPrincipal UserDetails userDetails) {
        org.example.batuku.domain.User user = (userDetails != null)
                ? jwtUserDetailsService.loadUserEntity(userDetails.getUsername())
                : null;
        return ResponseEntity.ok(artistProfileService.getSuggestedArtists(user));
    }
}
