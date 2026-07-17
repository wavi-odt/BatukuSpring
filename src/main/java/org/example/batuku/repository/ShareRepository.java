package org.example.batuku.repository;

import org.example.batuku.domain.Share;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareRepository extends JpaRepository<Share, Long> {
    long countByTrackId(Long trackId);
}
