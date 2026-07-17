package org.example.batuku.repository;

import org.example.batuku.domain.ArtistFollow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistFollowRepository extends JpaRepository<ArtistFollow, Long> {
    boolean existsByFollowerIdAndArtistProfileId(Long followerId, Long artistProfileId);
    void deleteByFollowerIdAndArtistProfileId(Long followerId, Long artistProfileId);
    long countByArtistProfileId(Long artistProfileId);
}
