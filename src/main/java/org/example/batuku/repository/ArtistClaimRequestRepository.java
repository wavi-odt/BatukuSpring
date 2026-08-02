package org.example.batuku.repository;

import org.example.batuku.domain.ArtistClaimRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtistClaimRequestRepository extends JpaRepository<ArtistClaimRequest, Long> {
    List<ArtistClaimRequest> findByStatusOrderByCreatedAtAsc(ArtistClaimRequest.ClaimStatus status);
    Optional<ArtistClaimRequest> findFirstByUserIdAndStatus(Long userId, ArtistClaimRequest.ClaimStatus status);
    boolean existsByArtistProfileIdAndStatus(Long artistProfileId, ArtistClaimRequest.ClaimStatus status);
    List<ArtistClaimRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}
