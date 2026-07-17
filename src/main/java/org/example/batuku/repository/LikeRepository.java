package org.example.batuku.repository;

import org.example.batuku.domain.Like;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByUserIdAndTrackId(Long userId, Long trackId);
    long countByTrackId(Long trackId);
    void deleteByUserIdAndTrackId(Long userId, Long trackId);
}
