package org.example.batuku.repository;

import org.example.batuku.domain.UserPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface UserPointsRepository extends JpaRepository<UserPoints, Long> {
    Optional<UserPoints> findByUserId(Long userId);

    // Apenas fãs participam no ranking
    @Query("SELECT up FROM UserPoints up WHERE up.user.userRole = 'FAN' ORDER BY up.totalPoints DESC, up.user.id ASC")
    List<UserPoints> findTopByOrderByTotalPointsDesc(Pageable pageable);

    // Rank = quantos fãs têm MAIS pontos + 1
    @Query("SELECT COUNT(up) + 1 FROM UserPoints up WHERE up.user.userRole = 'FAN' AND up.totalPoints > (SELECT u2.totalPoints FROM UserPoints u2 WHERE u2.user.id = :userId)")
    long findRankByUserId(Long userId);
}
