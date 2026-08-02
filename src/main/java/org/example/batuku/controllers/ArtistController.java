package org.example.batuku.controllers;

import jakarta.validation.Valid;
import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.User;
import org.example.batuku.dto.*;
import org.example.batuku.exception.SpotifyApiException;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.FollowRepository;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.repository.UserRepository;
import org.example.batuku.services.SpotifyClient;
import org.example.batuku.utils.BatukuOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    private final UserRepository userRepository;

    public ArtistController(ArtistProfileRepository artistProfileRepository,
                            TrackRepository trackRepository,
                            LikeRepository likeRepository,
                            FollowRepository followRepository,
                            SpotifyClient spotifyClient,
                            UserRepository userRepository) {
        this.artistProfileRepository = artistProfileRepository;
        this.trackRepository = trackRepository;
        this.likeRepository = likeRepository;
        this.followRepository = followRepository;
        this.spotifyClient = spotifyClient;
        this.userRepository = userRepository;
    }

    /** Resolve o artista autenticado ou devolve null se não autenticado / sem perfil. */
    private ArtistProfile resolveMyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) return null;
        return userRepository.findByEmail(auth.getName())
                .flatMap(u -> artistProfileRepository.findByUserId(u.getId()))
                .orElse(null);
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

    /** GET /api/artists/options — listas pré-definidas (público). */
    @GetMapping("/options")
    public ArtistOptionsResponse getOptions() {
        return ArtistOptionsResponse.defaults();
    }

    /** GET /api/artists/me — perfil editável do artista autenticado. */
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        ArtistProfile profile = resolveMyProfile();
        if (profile == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Não autenticado ou sem perfil de artista."));
        return ResponseEntity.ok(ArtistMeResponse.from(profile));
    }

    /** PUT /api/artists/me/bio */
    @PutMapping("/me/bio")
    public ResponseEntity<?> updateBio(@Valid @RequestBody ArtistBioRequest request) {
        ArtistProfile profile = resolveMyProfile();
        if (profile == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        profile.setBio(request.getBio());
        artistProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of("message", "Bio atualizada."));
    }

    /** PUT /api/artists/me/location — só aceita localizações da lista canónica. */
    @PutMapping("/me/location")
    public ResponseEntity<?> updateLocation(@Valid @RequestBody ArtistLocationRequest request) {
        ArtistProfile profile = resolveMyProfile();
        if (profile == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!BatukuOptions.isValidLocation(request.getLocation())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Localização inválida. Escolhe uma da lista disponível."));
        }
        profile.setLocation(request.getLocation());
        artistProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of("message", "Localização atualizada."));
    }

    /** PUT /api/artists/me/genres — máximo 3, apenas géneros da lista canónica. */
    @PutMapping("/me/genres")
    public ResponseEntity<?> updateGenres(@Valid @RequestBody ArtistGenresRequest request) {
        ArtistProfile profile = resolveMyProfile();
        if (profile == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<String> invalid = request.getGenres().stream()
                .filter(g -> !BatukuOptions.isValidGenre(g))
                .toList();
        if (!invalid.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Género(s) inválido(s): " + invalid));
        }
        profile.setGenres(request.getGenres());
        artistProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of("message", "Géneros atualizados."));
    }

    /** PUT /api/artists/me/languages — apenas línguas da lista canónica. */
    @PutMapping("/me/languages")
    public ResponseEntity<?> updateLanguages(@Valid @RequestBody ArtistLanguagesRequest request) {
        ArtistProfile profile = resolveMyProfile();
        if (profile == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<String> invalid = request.getLanguages().stream()
                .filter(l -> !BatukuOptions.isValidLanguage(l))
                .toList();
        if (!invalid.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Língua(s) inválida(s): " + invalid));
        }
        profile.setLanguages(request.getLanguages());
        artistProfileRepository.save(profile);
        return ResponseEntity.ok(Map.of("message", "Línguas atualizadas."));
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
        } catch (SpotifyApiException e) {
            log.warn("top-tracks: top-tracks endpoint failed (HTTP {}), falling back to search", e.getHttpStatus());
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
            } catch (SpotifyApiException ex) {
                log.warn("top-tracks: search fallback also failed (HTTP {})", ex.getHttpStatus());
                tracks = List.of();
            }
        }

        return ResponseEntity.ok(tracks);
    }
}
