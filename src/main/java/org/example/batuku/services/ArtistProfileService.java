package org.example.batuku.services;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.ArtistSocialLink;
import org.example.batuku.domain.Genre;
import org.example.batuku.domain.Track;
import org.example.batuku.domain.UnmappedGenre;
import org.example.batuku.domain.User;
import org.example.batuku.dto.ArtistDetailResponse;
import org.example.batuku.dto.ArtistFollowResponse;
import org.example.batuku.dto.ArtistOptionsResponse;
import org.example.batuku.dto.SimilarArtistResponse;
import org.example.batuku.dto.SpotifyTrackResponse;
import org.example.batuku.exception.SpotifyApiException;
import org.example.batuku.repository.ArtistFollowRepository;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.GenreRepository;
import org.example.batuku.repository.LanguageRepository;
import org.example.batuku.repository.LikeRepository;
import org.example.batuku.repository.LocationRepository;
import org.example.batuku.repository.PlayRepository;
import org.example.batuku.repository.TrackRepository;
import org.example.batuku.repository.UnmappedGenreRepository;
import org.example.batuku.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ArtistProfileService {

    private static final Logger log = LoggerFactory.getLogger(ArtistProfileService.class);

    private final ArtistProfileRepository artistProfileRepository;
    private final SpotifyClient spotifyClient;
    private final TrackRepository trackRepository;
    private final GenreRepository genreRepository;
    private final UnmappedGenreRepository unmappedGenreRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final PlayRepository playRepository;
    private final LanguageRepository languageRepository;
    private final LocationRepository locationRepository;

    public ArtistProfileService(ArtistProfileRepository artistProfileRepository,
                                SpotifyClient spotifyClient,
                                TrackRepository trackRepository,
                                GenreRepository genreRepository,
                                UnmappedGenreRepository unmappedGenreRepository,
                                UserRepository userRepository,
                                LikeRepository likeRepository,
                                ArtistFollowRepository artistFollowRepository,
                                PlayRepository playRepository,
                                LanguageRepository languageRepository,
                                LocationRepository locationRepository) {
        this.artistProfileRepository  = artistProfileRepository;
        this.spotifyClient            = spotifyClient;
        this.trackRepository          = trackRepository;
        this.genreRepository          = genreRepository;
        this.unmappedGenreRepository  = unmappedGenreRepository;
        this.userRepository           = userRepository;
        this.likeRepository           = likeRepository;
        this.artistFollowRepository   = artistFollowRepository;
        this.playRepository           = playRepository;
        this.languageRepository       = languageRepository;
        this.locationRepository       = locationRepository;
    }

    // ── Registos existentes (admin / Spotify) ─────────────────────────────

    public record ImportResult(ArtistProfile profile, int tracksImported, int tracksUpdated, int tracksSkipped, List<String> unmappedGenres) {}
    private record MappedGenres(List<String> mapped, List<String> unmapped) {}
    private record TrackStats(int imported, int updated, int skipped) {}

    public record HeroArtistDto(Long id, String name, String imageUrl) {}
    public record AdminArtistListItem(Long id, String name, String imageUrl, boolean featuredOnHero, String genre, String location) {}
    public record UnmappedGenreDto(Long id, String name, int occurrences) {}

    private static final java.util.regex.Pattern SPOTIFY_URL_PATTERN =
            java.util.regex.Pattern.compile("open\\.spotify\\.com/(?:intl-[a-z]{2}/)?artist/([A-Za-z0-9]{22})");
    private static final java.util.regex.Pattern SPOTIFY_ID_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9]{22}$");

    public List<SpotifyClient.SpotifyArtist> search(String query) {
        String trimmed = query.trim();
        String spotifyId = extractSpotifyId(trimmed);
        if (spotifyId != null) {
            return List.of(spotifyClient.getArtist(spotifyId));
        }
        return spotifyClient.searchArtists(trimmed, 10);
    }

    private String extractSpotifyId(String input) {
        var urlMatcher = SPOTIFY_URL_PATTERN.matcher(input);
        if (urlMatcher.find()) return urlMatcher.group(1);
        if (SPOTIFY_ID_PATTERN.matcher(input).matches()) return input;
        return null;
    }

    public List<HeroArtistDto> getHeroArtists() {
        return artistProfileRepository.findByFeaturedOnHeroTrueOrderByNameAsc().stream()
                .map(p -> new HeroArtistDto(p.getId(), p.getName(), p.getImageUrl()))
                .toList();
    }

    public List<AdminArtistListItem> listAll() {
        return artistProfileRepository.findAllByOrderByNameAsc().stream()
                .map(p -> {
                    String genre = (p.getGenres() != null && !p.getGenres().isEmpty()) ? p.getGenres().get(0) : null;
                    return new AdminArtistListItem(p.getId(), p.getName(), p.getImageUrl(), p.isFeaturedOnHero(), genre, p.getLocation());
                })
                .toList();
    }

    @Transactional
    public void setFeaturedOnHero(Long id, boolean featured) {
        ArtistProfile p = artistProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado: " + id));
        p.setFeaturedOnHero(featured);
        artistProfileRepository.save(p);
    }

    public Set<String> findImportedIds(List<String> spotifyIds) {
        return artistProfileRepository.findImportedSpotifyIds(spotifyIds);
    }

    public ImportResult findExisting(String spotifyArtistId) {
        return artistProfileRepository.findBySpotifyArtistId(spotifyArtistId)
                .map(p -> new ImportResult(p, 0, 0, 0, List.of()))
                .orElseThrow();
    }

    @Transactional
    public ImportResult importArtist(String spotifyArtistId) {
        SpotifyClient.SpotifyArtist artist = spotifyClient.getArtist(spotifyArtistId);

        ArtistProfile profile = artistProfileRepository.findBySpotifyArtistId(spotifyArtistId)
                .orElseGet(() -> {
                    ArtistProfile p = new ArtistProfile();
                    p.setSpotifyArtistId(artist.id());
                    p.setClaimed(false);
                    return p;
                });

        MappedGenres genres = mapToCanonical(artist.genres());
        profile.setName(artist.name());
        profile.setImageUrl(artist.imageUrl());
        profile.setThumbnailUrl(artist.thumbnailUrl());
        profile.setSpotifyUrl(artist.spotifyUrl());
        profile.setGenres(genres.mapped());
        profile.setSpotifyGenres(artist.genres() != null ? artist.genres() : List.of());
        profile.setPopularity(artist.popularity());
        profile.setFollowerCount(artist.followers() != null ? artist.followers().total() : null);
        profile = artistProfileRepository.save(profile);

        persistUnmapped(genres.unmapped());
        TrackStats stats = importTopTracks(profile);
        return new ImportResult(profile, stats.imported(), stats.updated(), stats.skipped(), genres.unmapped());
    }

    private TrackStats importTopTracks(ArtistProfile profile) {
        List<SpotifyClient.SpotifyTrack> tracks;
        try {
            tracks = spotifyClient.getTopTracks(profile.getSpotifyArtistId(), null);
        } catch (SpotifyApiException e) {
            log.warn("Failed to fetch top tracks for artist {}: {}", profile.getName(), e.getMessage());
            return new TrackStats(0, 0, 0);
        }

        int imported = 0, updated = 0, skipped = 0;
        for (SpotifyClient.SpotifyTrack t : tracks) {
            if (t.previewUrl() == null) {
                skipped++;
                continue;
            }

            Track track = trackRepository.findBySpotifyTrackId(t.id()).orElseGet(Track::new);
            boolean isNew = track.getSpotifyTrackId() == null;

            track.setTitle(t.name());
            track.setSource(Track.TrackSource.SPOTIFY_PREVIEW);
            track.setAudioUrl(t.previewUrl());
            track.setSpotifyTrackId(t.id());
            track.setSpotifyUrl(t.externalUrls() != null ? t.externalUrls().spotify() : null);
            track.setCoverUrl(t.album() != null ? t.album().coverUrl() : null);
            track.setDurationMs(t.durationMs());
            track.setArtistProfile(profile);
            track.setPublished(true);
            trackRepository.save(track);

            if (isNew) imported++; else updated++;
        }
        log.info("Artist '{}': {} track(s) imported, {} updated, {} skipped (no preview URL)",
                profile.getName(), imported, updated, skipped);
        return new TrackStats(imported, updated, skipped);
    }

    private void persistUnmapped(List<String> unmapped) {
        for (String name : unmapped) {
            unmappedGenreRepository.findByNameIgnoreCase(name).ifPresentOrElse(
                    existing -> { existing.incrementOccurrences(); unmappedGenreRepository.save(existing); },
                    () -> unmappedGenreRepository.save(new UnmappedGenre(name))
            );
        }
    }

    private MappedGenres mapToCanonical(List<String> spotifyGenres) {
        if (spotifyGenres == null || spotifyGenres.isEmpty()) return new MappedGenres(List.of(), List.of());
        List<String> canonical = genreRepository.findAll().stream()
                .map(Genre::getName)
                .toList();
        List<String> mapped   = new java.util.ArrayList<>();
        List<String> unmapped = new java.util.ArrayList<>();
        for (String sg : spotifyGenres) {
            canonical.stream().filter(c -> c.equalsIgnoreCase(sg)).findFirst()
                    .ifPresentOrElse(mapped::add, () -> unmapped.add(sg));
        }
        return new MappedGenres(mapped.stream().distinct().toList(), unmapped.stream().distinct().toList());
    }

    public List<UnmappedGenreDto> listUnmappedGenres() {
        return unmappedGenreRepository.findAllByOrderByOccurrencesDescNameAsc().stream()
                .map(u -> new UnmappedGenreDto(u.getId(), u.getName(), u.getOccurrences()))
                .toList();
    }

    @Transactional
    public void promoteUnmappedGenre(Long id) {
        UnmappedGenre u = unmappedGenreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Género não encontrado: " + id));

        String canonicalName = genreRepository.findByNameIgnoreCase(u.getName())
                .map(Genre::getName)
                .orElseGet(() -> {
                    Genre genre = new Genre();
                    genre.setName(u.getName());
                    genre.setHue(0);
                    genre.setCaboverdean(false);
                    return genreRepository.save(genre).getName();
                });

        List<ArtistProfile> affected = artistProfileRepository.findBySpotifyGenreIgnoreCase(u.getName());
        for (ArtistProfile p : affected) {
            List<String> current = p.getGenres() != null ? new java.util.ArrayList<>(p.getGenres()) : new java.util.ArrayList<>();
            if (current.stream().noneMatch(g -> g.equalsIgnoreCase(canonicalName))) {
                current.add(canonicalName);
                p.setGenres(current);
                artistProfileRepository.save(p);
            }
        }
        log.info("Promoted genre '{}': added to {} artist profile(s)", canonicalName, affected.size());

        unmappedGenreRepository.delete(u);
    }

    @Transactional
    public void dismissUnmappedGenre(Long id) {
        unmappedGenreRepository.deleteById(id);
    }

    // ── Métodos para ArtistController ─────────────────────────────────────

    /** Devolve o ArtistProfile associado ao e-mail do utilizador, se existir. */
    @Transactional(readOnly = true)
    public Optional<ArtistProfile> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .flatMap(u -> artistProfileRepository.findByUserId(u.getId()));
    }

    /** GET /api/artists/{id} */
    @Transactional(readOnly = true)
    public ArtistDetailResponse getArtistDetail(Long id) {
        ArtistProfile profile = artistProfileRepository.findById(id).orElseThrow();
        List<Track> tracks = trackRepository.findByArtistProfileId(id);

        long followers        = artistFollowRepository.countByArtistProfileId(id);
        long monthlyListeners = playRepository.countDistinctListenersByArtistSince(id, LocalDateTime.now().minusDays(30));

        String imageUrl = (profile.isClaimed()
                && profile.getUser() != null
                && profile.getUser().getAvatarUrl() != null)
                ? profile.getUser().getAvatarUrl()
                : profile.getImageUrl();

        List<String> genres = profile.getGenres() != null ? profile.getGenres() : List.of();
        String genre = genres.isEmpty() ? null : genres.get(0);

        List<ArtistDetailResponse.TrackItem> trackItems = tracks.stream()
                .map(t -> new ArtistDetailResponse.TrackItem(
                        t.getId(),
                        t.getTitle(),
                        t.getCoverUrl(),
                        likeRepository.countByTrackId(t.getId()),
                        formatDuration(t.getDurationMs())
                ))
                .toList();

        List<ArtistDetailResponse.LinkItem> linkItems = (profile.getLinks() != null)
                ? profile.getLinks().stream()
                        .filter(l -> l.getKind() != null && !l.getKind().isBlank())
                        .map(l -> new ArtistDetailResponse.LinkItem(l.getKind(), l.getHandle() != null ? l.getHandle() : ""))
                        .toList()
                : List.of();

        Long userId = (profile.isClaimed() && profile.getUser() != null)
                ? profile.getUser().getId() : null;
        String username = (profile.isClaimed() && profile.getUser() != null)
                ? profile.getUser().getUsername() : null;

        return new ArtistDetailResponse(
                profile.getId(),
                profile.getName(),
                imageUrl,
                genre,
                profile.getLocation(),
                profile.getBio(),
                followers,
                monthlyListeners,
                tracks.size(),
                trackItems,
                genres,
                profile.getLanguages() != null ? profile.getLanguages() : List.of(),
                linkItems,
                userId,
                username,
                profile.isClaimed()
        );
    }

    /** GET /api/artists/options */
    @Transactional(readOnly = true)
    public ArtistOptionsResponse getOptions() {
        List<String> genres = genreRepository.findAll().stream()
                .map(Genre::getName).sorted().toList();
        List<String> languages = languageRepository.findAll().stream()
                .map(org.example.batuku.domain.Language::getName).toList();
        List<ArtistOptionsResponse.LocationDto> locations = locationRepository.findAll().stream()
                .map(l -> new ArtistOptionsResponse.LocationDto(l.getValue(), l.getLocationGroup()))
                .toList();
        return new ArtistOptionsResponse(genres, languages, locations);
    }

    /** PUT /api/artists/me/name */
    @Transactional
    public String updateName(ArtistProfile profile, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome artístico não pode estar vazio.");
        }
        String trimmed = name.trim();
        profile.setName(trimmed);
        artistProfileRepository.save(profile);
        return trimmed;
    }

    /** PUT /api/artists/me/bio */
    @Transactional
    public void updateBio(ArtistProfile profile, String bio) {
        profile.setBio(bio);
        artistProfileRepository.save(profile);
    }

    /** PUT /api/artists/me/location */
    @Transactional
    public void updateLocation(ArtistProfile profile, String location) {
        if (!locationRepository.existsByValue(location)) {
            throw new IllegalArgumentException("Localização inválida. Escolhe uma da lista disponível.");
        }
        profile.setLocation(location);
        artistProfileRepository.save(profile);
    }

    /** PUT /api/artists/me/genres */
    @Transactional
    public void updateGenres(ArtistProfile profile, List<String> genres) {
        Set<String> validNames = genreRepository.findAll().stream()
                .map(Genre::getName)
                .collect(Collectors.toSet());
        List<String> invalid = genres.stream()
                .filter(g -> !validNames.contains(g))
                .toList();
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("Género(s) inválido(s): " + invalid);
        }
        profile.setGenres(genres);
        artistProfileRepository.save(profile);
    }

    /** PUT /api/artists/me/languages */
    @Transactional
    public void updateLanguages(ArtistProfile profile, List<String> languages) {
        Set<String> validLangs = languageRepository.findAll().stream()
                .map(org.example.batuku.domain.Language::getName)
                .collect(Collectors.toSet());
        List<String> invalid = languages.stream()
                .filter(l -> !validLangs.contains(l))
                .toList();
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("Língua(s) inválida(s): " + invalid);
        }
        profile.setLanguages(languages);
        artistProfileRepository.save(profile);
    }

    /** PUT /api/artists/me/links */
    @Transactional
    public void updateLinks(ArtistProfile profile, List<Map<String, String>> social) {
        List<ArtistSocialLink> links = new ArrayList<>(social.stream()
                .filter(s -> s.get("kind") != null && !s.get("kind").isBlank())
                .map(s -> new ArtistSocialLink(s.get("kind"), s.getOrDefault("handle", "")))
                .toList());
        profile.setLinks(links);
        artistProfileRepository.save(profile);
    }

    /** GET /api/artists/{id}/top-tracks */
    public List<SpotifyTrackResponse> getTopTracks(Long id, String market) {
        ArtistProfile profile = artistProfileRepository.findById(id).orElseThrow();

        log.info("top-tracks: artistId={} spotifyId={}", id, profile.getSpotifyArtistId());

        if (profile.getSpotifyArtistId() == null) {
            log.info("top-tracks: spotifyArtistId is null, returning empty list");
            return List.of();
        }

        try {
            List<SpotifyTrackResponse> tracks = spotifyClient
                    .getTopTracks(profile.getSpotifyArtistId(), market)
                    .stream()
                    .map(this::toSpotifyTrackResponse)
                    .toList();
            log.info("top-tracks: Spotify returned {} tracks", tracks.size());
            return tracks;
        } catch (SpotifyApiException e) {
            log.warn("top-tracks: top-tracks endpoint failed (HTTP {}), falling back to filtered search", e.getHttpStatus());
            try {
                List<SpotifyTrackResponse> tracks = spotifyClient
                        .searchTracksByArtistId(profile.getSpotifyArtistId(), profile.getName(), 10, market)
                        .stream()
                        .map(this::toSpotifyTrackResponse)
                        .toList();
                log.info("top-tracks: filtered search returned {} tracks", tracks.size());
                return tracks;
            } catch (SpotifyApiException ex) {
                log.warn("top-tracks: filtered search also failed (HTTP {})", ex.getHttpStatus());
                return List.of();
            }
        }
    }

    private SpotifyTrackResponse toSpotifyTrackResponse(SpotifyClient.SpotifyTrack t) {
        return new SpotifyTrackResponse(
                t.id(),
                t.name(),
                t.previewUrl(),
                t.durationMs(),
                t.externalUrls() != null ? t.externalUrls().spotify() : null,
                t.album() != null ? t.album().coverUrl() : null
        );
    }

    /** GET /api/artists/{id}/similar */
    @Transactional(readOnly = true)
    public List<SimilarArtistResponse> getSimilarArtists(Long id, int limit) {
        ArtistProfile target = artistProfileRepository.findById(id).orElseThrow();
        List<String> targetGenresLower = target.getGenres() != null
                ? target.getGenres().stream().map(String::toLowerCase).toList()
                : List.of();

        List<ArtistProfile> others = artistProfileRepository.findAll().stream()
                .filter(a -> !a.getId().equals(id))
                .toList();

        List<ArtistProfile> matched = targetGenresLower.isEmpty() ? List.of() : others.stream()
                .filter(a -> a.getGenres() != null && a.getGenres().stream()
                        .anyMatch(g -> targetGenresLower.contains(g.toLowerCase())))
                .toList();

        List<ArtistProfile> candidates = matched.isEmpty() ? others : matched;

        return candidates.stream()
                .map(a -> {
                    String imageUrl = (a.isClaimed() && a.getUser() != null && a.getUser().getAvatarUrl() != null)
                            ? a.getUser().getAvatarUrl()
                            : a.getImageUrl();
                    String g = (a.getGenres() != null && !a.getGenres().isEmpty()) ? a.getGenres().get(0) : null;
                    long totalPlays = playRepository.countTotalPlaysByArtist(a.getId());
                    return new SimilarArtistResponse(a.getId(), a.getName(), imageUrl, g, totalPlays);
                })
                .sorted((x, y) -> Long.compare(y.totalPlays(), x.totalPlays()))
                .limit(limit)
                .toList();
    }

    /** GET /api/artists/suggested */
    @Transactional(readOnly = true)
    public List<ArtistFollowResponse> getSuggestedArtists(User user) {
        Set<Long> followedIds = Set.of();
        if (user != null) {
            followedIds = artistFollowRepository.findByFollowerIdOrderByCreatedAtDesc(user.getId())
                    .stream().map(f -> f.getArtistProfile().getId())
                    .collect(Collectors.toSet());
        }
        final Set<Long> excluded = followedIds;
        final LocalDateTime since30d = LocalDateTime.now().minusDays(30);

        return artistProfileRepository.findAll().stream()
                .filter(a -> !excluded.contains(a.getId()))
                .map(a -> {
                    long followers      = artistFollowRepository.countByArtistProfileId(a.getId());
                    long recentPlays    = playRepository.countByTrackArtistProfileIdAndPlayedAtAfter(a.getId(), since30d);
                    long recentListeners= playRepository.countDistinctListenersByArtistSince(a.getId(), since30d);
                    long recentLikes    = likeRepository.countByTrackArtistProfileIdAndCreatedAtAfter(a.getId(), since30d);
                    long recentFollows  = artistFollowRepository.countByArtistProfileIdAndCreatedAtAfter(a.getId(), since30d);
                    double score = followers       * 5.0
                                 + recentListeners * 4.0
                                 + recentFollows   * 3.0
                                 + recentLikes     * 2.0
                                 + recentPlays     * 0.5;
                    return Map.entry(ArtistFollowResponse.from(a, followers, recentListeners), score);
                })
                .filter(e -> e.getKey().monthlyListeners() >= 1)
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static String formatDuration(Integer ms) {
        if (ms == null) return null;
        int totalSec = ms / 1000;
        return totalSec / 60 + ":" + String.format("%02d", totalSec % 60);
    }
}
