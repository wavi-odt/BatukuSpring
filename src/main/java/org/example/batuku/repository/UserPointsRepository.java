package org.example.batuku.repository;

import org.example.batuku.domain.UserPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserPointsRepository extends JpaRepository<UserPoints, Long> {
    Optional<UserPoints> findByUserId(Long userId);
}
