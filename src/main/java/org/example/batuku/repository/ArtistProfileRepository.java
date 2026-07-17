package org.example.batuku.repository;

import org.example.batuku.domain.ArtistProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Long> {
    boolean existsBySpotifyArtistId(String spotifyArtistId);
    Optional<ArtistProfile> findBySpotifyArtistId(String spotifyArtistId);
    Optional<ArtistProfile> findByUserId(Long userId);
    List<ArtistProfile> findTop5ByNameContainingIgnoreCase(String name);
}
