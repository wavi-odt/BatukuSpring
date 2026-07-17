package org.example.batuku.controllers;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.SearchResponse;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "${batuku.cors.allowed-origin}")
public class SearchController {

    private final ArtistProfileRepository artistProfileRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;

    public SearchController(ArtistProfileRepository artistProfileRepository,
                            TrackRepository trackRepository,
                            UserRepository userRepository) {
        this.artistProfileRepository = artistProfileRepository;
        this.trackRepository = trackRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public SearchResponse search(@RequestParam("q") String q) {
        if (q == null || q.strip().length() < 2) {
            return new SearchResponse(List.of(), List.of(), List.of(), List.of());
        }

        String term = q.strip();

        List<SearchResponse.ArtistResult> artists = artistProfileRepository
                .findTop5ByNameContainingIgnoreCase(term)
                .stream()
                .map(this::toArtistResult)
                .toList();

        List<SearchResponse.TrackResult> tracks = trackRepository
                .findTop5ByTitleContainingIgnoreCase(term)
                .stream()
                .map(this::toTrackResult)
                .toList();

        List<SearchResponse.UserResult> users = userRepository
                .findTop5ByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(term, term)
                .stream()
                .map(this::toUserResult)
                .toList();

        return new SearchResponse(artists, tracks, List.of(), users);
    }

    private SearchResponse.ArtistResult toArtistResult(ArtistProfile p) {
        String genre = (p.getGenres() != null && !p.getGenres().isEmpty()) ? p.getGenres().get(0) : null;
        return new SearchResponse.ArtistResult(p.getId(), p.getName(), p.getImageUrl(), genre);
    }

    private SearchResponse.TrackResult toTrackResult(Track t) {
        return new SearchResponse.TrackResult(
                t.getId(),
                t.getTitle(),
                t.getArtistProfile().getName(),
                t.getCoverUrl(),
                formatDuration(t.getDurationMs())
        );
    }

    private SearchResponse.UserResult toUserResult(User u) {
        return new SearchResponse.UserResult(u.getId(), u.getName(), "@" + u.getUsername(), u.getAvatarUrl());
    }

    static String formatDuration(Integer ms) {
        if (ms == null) return null;
        int totalSec = ms / 1000;
        return totalSec / 60 + ":" + String.format("%02d", totalSec % 60);
    }
}
