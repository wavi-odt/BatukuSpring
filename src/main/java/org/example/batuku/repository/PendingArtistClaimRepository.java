package org.example.batuku.repository;

import org.example.batuku.domain.PendingArtistClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PendingArtistClaimRepository extends JpaRepository<PendingArtistClaim, Long> {
    List<PendingArtistClaim> findByStatusOrderByCreatedAtAsc(PendingArtistClaim.ClaimStatus status);
    boolean existsByEmailAndStatus(String email, PendingArtistClaim.ClaimStatus status);
    long countByStatus(PendingArtistClaim.ClaimStatus status);

    @Modifying
    @Query("UPDATE PendingArtistClaim c SET c.reviewedBy = null WHERE c.reviewedBy.id = :userId")
    void clearReviewedBy(Long userId);
}
