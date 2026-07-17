package org.example.batuku.services;

import org.example.batuku.domain.ArtistProfile;
import org.example.batuku.domain.Track;
import org.example.batuku.repository.ArtistProfileRepository;
import org.example.batuku.repository.TrackRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArtistProfileService {

    private final ArtistProfileRepository artistProfileRepository;
    private final SpotifyClient spotifyClient;
    private final TrackRepository trackRepository;
    private final String market;

    public ArtistProfileService(ArtistProfileRepository artistProfileRepository,
                                SpotifyClient spotifyClient,
                                TrackRepository trackRepository,
                                @Value("${batuku.spotify.market}") String market) {
        this.artistProfileRepository = artistProfileRepository;
        this.spotifyClient = spotifyClient;
        this.trackRepository = trackRepository;
        this.market = market;
    }

    public List<SpotifyClient.SpotifyArtist> search(String query) {
        return spotifyClient.searchArtists(query, 5);
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
        List<SpotifyClient.SpotifyTrack> tracks =
                spotifyClient.getTopTracks(profile.getSpotifyArtistId(), market);

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
