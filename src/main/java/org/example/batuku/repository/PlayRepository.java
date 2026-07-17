package org.example.batuku.repository;

import org.example.batuku.domain.Play;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayRepository extends JpaRepository<Play, Long> {
    long countByTrackId(Long trackId);
}
