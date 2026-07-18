package org.example.batuku.controllers;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.dto.ArtistDetailResponse;
import org.example.batuku.dto.SpotifyTrackResponse;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.FollowRepository;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.services.ItunesClient;
import org.example.batuku.services.SpotifyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@RestController
@RequestMapping("/api/artists")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class ArtistController {

    private static final Logger log = LoggerFactory.getLogger(ArtistController.class);

    private final ArtistProfileRepository artistProfileRepository;
    private final TrackRepository trackRepository;
    private final LikeRepository likeRepository;
    private final FollowRepository followRepository;
    private final SpotifyClient spotifyClient;

    public ArtistController(ArtistProfileRepository artistProfileRepository,
                            TrackRepository trackRepository,
                            LikeRepository likeRepository,
                            FollowRepository followRepository,
                            SpotifyClient spotifyClient) {
        this.artistProfileRepository = artistProfileRepository;
        this.trackRepository = trackRepository;
        this.likeRepository = likeRepository;
        this.followRepository = followRepository;
        this.spotifyClient = spotifyClient;
    }

    @GetMapping("/{id}")
    public ArtistDetailResponse getArtist(@PathVariable Long id) {
        ArtistProfile profile = artistProfileRepository.findById(id).orElseThrow();

        List<Track> tracks = trackRepository.findByArtistProfileId(id);

        long followers = profile.getUser() != null
                ? followRepository.countByFolloweeId(profile.getUser().getId())
                : 0L;

        String genre = (profile.getGenres() != null && !profile.getGenres().isEmpty())
                ? profile.getGenres().get(0)
                : null;

        List<ArtistDetailResponse.TrackItem> trackItems = tracks.stream()
                .map(t -> new ArtistDetailResponse.TrackItem(
                        t.getId(),
                        t.getTitle(),
                        t.getCoverUrl(),
                        likeRepository.countByTrackId(t.getId()),
                        SearchController.formatDuration(t.getDurationMs())
                ))
                .toList();

        return new ArtistDetailResponse(
                profile.getId(),
                profile.getName(),
                profile.getImageUrl(),
                genre,
                null,
                null,
                followers,
                tracks.size(),
                trackItems
        );
    }

    @GetMapping("/{id}/top-tracks")
    public ResponseEntity<List<SpotifyTrackResponse>> getTopTracks(@PathVariable Long id) {

        ArtistProfile profile = artistProfileRepository.findById(id).orElseThrow();

        log.info("top-tracks: artistId={} spotifyId={}", id, profile.getSpotifyArtistId());

        if (profile.getSpotifyArtistId() == null) {
            log.info("top-tracks: spotifyArtistId is null, returning empty list");
            return ResponseEntity.ok(List.of());
        }

        List<SpotifyTrackResponse> tracks;
        try {
            tracks = spotifyClient
                    .getTopTracks(profile.getSpotifyArtistId())
                    .stream()
                    .map(t -> new SpotifyTrackResponse(
                            t.id(),
                            t.name(),
                            t.previewUrl(),
                            t.durationMs(),
                            t.externalUrls() != null ? t.externalUrls().spotify() : null,
                            t.album() != null ? t.album().coverUrl() : null
                    ))
                    .toList();
            log.info("top-tracks: Spotify returned {} tracks", tracks.size());
        } catch (HttpClientErrorException e) {
            log.warn("top-tracks: top-tracks endpoint failed ({}), falling back to search", e.getStatusCode());
            try {
                tracks = spotifyClient
                        .searchTracksByArtistName(profile.getName(), 10)
                        .stream()
                        .map(t -> new SpotifyTrackResponse(
                                t.id(),
                                t.name(),
                                t.previewUrl(),
                                t.durationMs(),
                                t.externalUrls() != null ? t.externalUrls().spotify() : null,
                                t.album() != null ? t.album().coverUrl() : null
                        ))
                        .toList();
                log.info("top-tracks: search fallback returned {} tracks", tracks.size());
            } catch (HttpClientErrorException ex) {
                log.warn("top-tracks: search fallback also failed: {}", ex.getStatusCode());
                tracks = List.of();
            }
        }

        return ResponseEntity.ok(tracks);
    }
}
