package org.example.batuku.repository;

import org.example.batuku.domain.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long> {
    List<Track> findByArtistProfileId(Long artistProfileId);
    List<Track> findByArtistProfileIdAndSource(Long artistProfileId, Track.TrackSource source);
    boolean existsBySpotifyTrackId(String spotifyTrackId);
    Optional<Track> findBySpotifyTrackId(String spotifyTrackId);
    List<Track> findTop5ByTitleContainingIgnoreCase(String title);
    List<Track> findTop5ByIsPublishedTrueOrderByCreatedAtDesc();
}
