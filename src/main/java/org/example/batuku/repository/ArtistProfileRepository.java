package org.example.batuku.repository;

import org.example.batuku.domain.ArtistProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Long> {
    boolean existsBySpotifyArtistId(String spotifyArtistId);
    Optional<ArtistProfile> findBySpotifyArtistId(String spotifyArtistId);
    Optional<ArtistProfile> findByUserId(Long userId);
    List<ArtistProfile> findTop5ByNameContainingIgnoreCase(String name);

    @Query("SELECT a.spotifyArtistId FROM ArtistProfile a WHERE a.spotifyArtistId IN :ids")
    Set<String> findImportedSpotifyIds(@Param("ids") List<String> ids);

    @Query("SELECT a FROM ArtistProfile a WHERE a.user IS NOT NULL AND a.user.id IN (SELECT f.followee.id FROM Follow f WHERE f.follower.id = :userId)")
    List<ArtistProfile> findByFollowerViaUserFollow(@Param("userId") Long userId);
}
