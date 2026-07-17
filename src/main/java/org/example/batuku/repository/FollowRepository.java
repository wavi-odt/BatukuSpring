package org.example.batuku.repository;

import org.example.batuku.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    List<Follow> findByFollowerId(Long followerId);
    List<Follow> findByFolloweeId(Long followeeId);
    long countByFolloweeId(Long followeeId);
    long countByFollowerId(Long followerId);
    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
}
