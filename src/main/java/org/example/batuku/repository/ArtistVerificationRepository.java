package org.example.batuku.repository;

import org.example.batuku.domain.ArtistVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArtistVerificationRepository extends JpaRepository<ArtistVerification, Long> {
    List<ArtistVerification> findByArtistProfileId(Long artistProfileId);
    boolean existsByArtistProfileIdAndPlatformArtistId(Long artistProfileId, String platformArtistId);
}
