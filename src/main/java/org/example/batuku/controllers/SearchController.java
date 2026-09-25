package org.example.batuku.controllers;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.SearchResponse;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

        // ── Artistas ────────────────────────────────────────────────────────
        List<ArtistProfile> allArtists = artistProfileRepository.findTop5ByNameContainingIgnoreCase(term);

        // Ligados a um utilizador real (independentemente de claimed)
        List<ArtistProfile> linkedArtists   = allArtists.stream()
                .filter(a -> a.getUser() != null)
                .toList();
        // Sem utilizador = importados pelo admin, disponíveis para reclamar
        List<ArtistProfile> unlinkedArtists = allArtists.stream()
                .filter(a -> a.getUser() == null)
                .toList();

        // userId → ArtistProfile para artistas com utilizador ligado
        Map<Long, ArtistProfile> userToArtistProfile = new java.util.HashMap<>(linkedArtists.stream()
                .collect(Collectors.toMap(a -> a.getUser().getId(), a -> a)));

        // ── Utilizadores ────────────────────────────────────────────────────
        List<User> rawUsers = userRepository
                .findTop5ByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(term, term);

        Set<Long> rawUserIds = rawUsers.stream().map(User::getId).collect(Collectors.toSet());

        // Garante que temos perfis de artista para todos os utilizadores encontrados,
        // mesmo que o seu perfil não tenha aparecido no top5 da pesquisa por nome.
        artistProfileRepository.findByUserIdIn(new ArrayList<>(rawUserIds))
                .forEach(ap -> userToArtistProfile.putIfAbsent(ap.getUser().getId(), ap));

        List<SearchResponse.UserResult> users = new ArrayList<>();

        // Utilizadores encontrados pela pesquisa de nome/username
        for (User u : rawUsers) {
            ArtistProfile ap = userToArtistProfile.get(u.getId());
            String imageUrl = u.getAvatarUrl() != null ? u.getAvatarUrl() : (ap != null ? ap.getImageUrl() : null);
            users.add(new SearchResponse.UserResult(
                    u.getId(), u.getName(), "@" + u.getUsername(), imageUrl,
                    ap != null ? ap.getId() : null));
        }

        // Artistas com utilizador ligado cujo utilizador NÃO apareceu na pesquisa por nome
        // (o nome do artista bate com o termo mas o nome do utilizador não)
        for (ArtistProfile a : linkedArtists) {
            User u = a.getUser();
            if (!rawUserIds.contains(u.getId())) {
                String imageUrl = u.getAvatarUrl() != null ? u.getAvatarUrl() : a.getImageUrl();
                users.add(new SearchResponse.UserResult(
                        u.getId(), u.getName(), "@" + u.getUsername(), imageUrl, a.getId()));
            }
        }

        // ── Resultado final ──────────────────────────────────────────────────
        List<SearchResponse.ArtistResult> artists = unlinkedArtists.stream()
                .map(this::toArtistResult)
                .toList();

        List<SearchResponse.TrackResult> tracks = trackRepository
                .findTop5ByTitleContainingIgnoreCase(term)
                .stream()
                .map(this::toTrackResult)
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

    static String formatDuration(Integer ms) {
        if (ms == null) return null;
        int totalSec = ms / 1000;
        return totalSec / 60 + ":" + String.format("%02d", totalSec % 60);
    }
}
