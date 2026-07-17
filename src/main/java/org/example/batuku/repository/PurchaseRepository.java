package org.example.batuku.repository;

import org.example.batuku.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByUserId(Long userId);
    boolean existsByUserIdAndBeatId(Long userId, Long beatId);
}
