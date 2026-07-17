package org.example.batuku.repository;

import org.example.batuku.domain.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserIdAndTargetTypeOrderByScoreDesc(Long userId, Recommendation.TargetType targetType);
}
