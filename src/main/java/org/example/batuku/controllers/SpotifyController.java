package org.example.batuku.controllers;

import org.example.batuku.services.ArtistProfileService;
import org.example.batuku.services.SpotifyClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spotify")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class SpotifyController {

    private final SpotifyClient spotifyClient;
    private final ArtistProfileService artistProfileService;

    public SpotifyController(SpotifyClient spotifyClient, ArtistProfileService artistProfileService) {
        this.spotifyClient = spotifyClient;
        this.artistProfileService = artistProfileService;
    }

    @GetMapping("/artists/{id}/top-tracks")
    public List<SpotifyTrackResult> getTopTracks(@PathVariable String id) {
        return spotifyClient.getTopTracks(id)
                .stream()
                .map(t -> new SpotifyTrackResult(
                        t.id(),
                        t.name(),
                        t.durationMs(),
                        t.previewUrl(),
                        t.externalUrls() != null ? t.externalUrls().spotify() : null,
                        t.album() != null ? t.album().coverUrl() : null
                ))
                .toList();
    }

    public record SpotifyTrackResult(
            String id,
            String name,
            Integer durationMs,
            String previewUrl,
            String externalUrl,
            String coverUrl
    ) {}

    @GetMapping("/search/artists")
    public List<SpotifyArtistResult> searchArtists(@RequestParam("q") String q) {
        return artistProfileService.search(q)
                .stream()
                .map(a -> new SpotifyArtistResult(
                        a.id(),
                        a.name(),
                        a.imageUrl(),
                        a.followers() != null ? a.followers().total() : null,
                        a.genres()
                ))
                .toList();
    }

    public record SpotifyArtistResult(
            String id,
            String name,
            String imageUrl,
            Integer followers,
            List<String> genres
    ) {}
}
