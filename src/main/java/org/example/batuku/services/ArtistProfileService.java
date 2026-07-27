package org.example.batuku.services;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.exception.SpotifyApiException;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.TrackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class ArtistProfileService {

    private static final Logger log = LoggerFactory.getLogger(ArtistProfileService.class);

    private final ArtistProfileRepository artistProfileRepository;
    private final SpotifyClient spotifyClient;
    private final TrackRepository trackRepository;

    public ArtistProfileService(ArtistProfileRepository artistProfileRepository,
                                SpotifyClient spotifyClient,
                                TrackRepository trackRepository) {
        this.artistProfileRepository = artistProfileRepository;
        this.spotifyClient = spotifyClient;
        this.trackRepository = trackRepository;
    }

    public record ImportResult(ArtistProfile profile, int tracksImported, int tracksUpdated, int tracksSkipped) {}
    private record TrackStats(int imported, int updated, int skipped) {}

    public List<SpotifyClient.SpotifyArtist> search(String query) {
        return spotifyClient.searchArtists(query, 5);
    }

    public Set<String> findImportedIds(List<String> spotifyIds) {
        return artistProfileRepository.findImportedSpotifyIds(spotifyIds);
    }

    public ImportResult findExisting(String spotifyArtistId) {
        return artistProfileRepository.findBySpotifyArtistId(spotifyArtistId)
                .map(p -> new ImportResult(p, 0, 0, 0))
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

        profile.setName(artist.name());
        profile.setImageUrl(artist.imageUrl());
        profile.setThumbnailUrl(artist.thumbnailUrl());
        profile.setSpotifyUrl(artist.spotifyUrl());
        profile.setGenres(artist.genres());
        profile.setPopularity(artist.popularity());
        profile.setFollowerCount(artist.followers() != null ? artist.followers().total() : null);
        profile = artistProfileRepository.save(profile);

        TrackStats stats = importTopTracks(profile);
        return new ImportResult(profile, stats.imported(), stats.updated(), stats.skipped());
    }

    private TrackStats importTopTracks(ArtistProfile profile) {
        List<SpotifyClient.SpotifyTrack> tracks;
        try {
            tracks = spotifyClient.getTopTracks(profile.getSpotifyArtistId());
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
}
