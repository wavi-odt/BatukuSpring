package org.example.batuku.repository;

import org.example.batuku.domain.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
    boolean existsByName(String name);
}
