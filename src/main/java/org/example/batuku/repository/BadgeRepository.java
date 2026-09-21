package org.example.batuku.repository;

import org.example.batuku.domain.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
    boolean existsByName(String name);
    List<Badge> findByPointsRequiredLessThanEqual(int points);
}
