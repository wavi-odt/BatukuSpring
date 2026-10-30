package org.example.batuku.services;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.SearchResponse;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final ArtistProfileRepository artistProfileRepository;
    private final TrackRepository         trackRepository;
    private final UserRepository          userRepository;

    public SearchService(ArtistProfileRepository artistProfileRepository,
                         TrackRepository trackRepository,
                         UserRepository userRepository) {
        this.artistProfileRepository = artistProfileRepository;
        this.trackRepository         = trackRepository;
        this.userRepository          = userRepository;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(String q) {
        if (q == null || q.strip().length() < 2) {
            return new SearchResponse(List.of(), List.of(), List.of(), List.of());
        }

        String term = q.strip();

        List<ArtistProfile> allArtists = artistProfileRepository.findTop5ByNameContainingIgnoreCase(term);

        List<ArtistProfile> linkedArtists   = allArtists.stream().filter(a -> a.getUser() != null).toList();
        List<ArtistProfile> unlinkedArtists = allArtists.stream().filter(a -> a.getUser() == null).toList();

        Map<Long, ArtistProfile> userToArtistProfile = new HashMap<>(linkedArtists.stream()
                .collect(Collectors.toMap(a -> a.getUser().getId(), a -> a)));

        List<User> rawUsers = userRepository
                .findTop5ByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(term, term)
                .stream()
                .filter(u -> u.getUserRole() != User.UserRole.ADMIN)
                .toList();

        Set<Long> rawUserIds = rawUsers.stream().map(User::getId).collect(Collectors.toSet());

        artistProfileRepository.findByUserIdIn(new ArrayList<>(rawUserIds))
                .forEach(ap -> userToArtistProfile.putIfAbsent(ap.getUser().getId(), ap));

        List<SearchResponse.UserResult> users = new ArrayList<>();

        for (User u : rawUsers) {
            ArtistProfile ap = userToArtistProfile.get(u.getId());
            String imageUrl = u.getAvatarUrl() != null ? u.getAvatarUrl() : (ap != null ? ap.getImageUrl() : null);
            users.add(new SearchResponse.UserResult(
                    u.getId(), ap != null ? ap.getName() : u.getName(), "@" + u.getUsername(), imageUrl,
                    ap != null ? ap.getId() : null));
        }

        for (ArtistProfile a : linkedArtists) {
            User u = a.getUser();
            if (!rawUserIds.contains(u.getId())) {
                String imageUrl = u.getAvatarUrl() != null ? u.getAvatarUrl() : a.getImageUrl();
                users.add(new SearchResponse.UserResult(
                        u.getId(), a.getName(), "@" + u.getUsername(), imageUrl, a.getId()));
            }
        }

        List<SearchResponse.ArtistResult> artists = unlinkedArtists.stream()
                .map(this::toArtistResult).toList();

        List<SearchResponse.TrackResult> tracks = trackRepository
                .findTop5ByTitleContainingIgnoreCase(term)
                .stream().map(this::toTrackResult).toList();

        return new SearchResponse(artists, tracks, List.of(), users);
    }

    private SearchResponse.ArtistResult toArtistResult(ArtistProfile p) {
        String genre = (p.getGenres() != null && !p.getGenres().isEmpty()) ? p.getGenres().get(0) : null;
        return new SearchResponse.ArtistResult(p.getId(), p.getName(), p.getImageUrl(), genre);
    }

    private SearchResponse.TrackResult toTrackResult(Track t) {
        return new SearchResponse.TrackResult(
                t.getId(), t.getTitle(), t.getArtistProfile().getName(),
                t.getCoverUrl(), formatDuration(t.getDurationMs()),
                t.getAudioUrl(), t.getArtistProfile().getId(),
                t.getSpotifyTrackId(), t.getDurationMs());
    }

    private static String formatDuration(Integer ms) {
        if (ms == null) return null;
        int totalSec = ms / 1000;
        return totalSec / 60 + ":" + String.format("%02d", totalSec % 60);
    }
}
