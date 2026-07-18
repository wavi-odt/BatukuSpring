package org.example.batuku.services;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.TrackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

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

    public List<SpotifyClient.SpotifyArtist> search(String query) {
        return spotifyClient.searchArtists(query, 5);
    }

    public Set<String> findImportedIds(List<String> spotifyIds) {
        return artistProfileRepository.findImportedSpotifyIds(spotifyIds);
    }

    @Transactional
    public ArtistProfile importArtist(String spotifyArtistId) {
        ArtistProfile profile = artistProfileRepository.findBySpotifyArtistId(spotifyArtistId)
                .orElseGet(() -> {
                    SpotifyClient.SpotifyArtist artist = spotifyClient.getArtist(spotifyArtistId);
                    ArtistProfile p = new ArtistProfile();
                    p.setSpotifyArtistId(artist.id());
                    p.setName(artist.name());
                    p.setImageUrl(artist.imageUrl());
                    p.setSpotifyUrl(artist.spotifyUrl());
                    p.setGenres(artist.genres());
                    p.setClaimed(false);
                    return artistProfileRepository.save(p);
                });

        importTopTracks(profile);
        return profile;
    }

    private void importTopTracks(ArtistProfile profile) {
        List<SpotifyClient.SpotifyTrack> tracks;
        try {
            tracks = spotifyClient.getTopTracks(profile.getSpotifyArtistId());
        } catch (HttpClientErrorException e) {
            log.warn("Failed to fetch top tracks for artist {}: {} {}",
                    profile.getSpotifyArtistId(), e.getStatusCode(), e.getMessage());
            return;
        }

        tracks.stream()
                .filter(t -> t.previewUrl() != null)
                .filter(t -> !trackRepository.existsBySpotifyTrackId(t.id()))
                .forEach(t -> {
                    Track track = new Track();
                    track.setTitle(t.name());
                    track.setSource(Track.TrackSource.SPOTIFY_PREVIEW);
                    track.setAudioUrl(t.previewUrl());
                    track.setSpotifyTrackId(t.id());
                    track.setSpotifyUrl(t.externalUrls() != null ? t.externalUrls().spotify() : null);
                    track.setCoverUrl(t.album() != null ? t.album().coverUrl() : null);
                    track.setDurationMs(t.durationMs());
                    track.setArtistProfile(profile);
                    trackRepository.save(track);
                });
    }
}
