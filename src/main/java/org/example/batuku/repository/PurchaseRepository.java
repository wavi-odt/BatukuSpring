package org.example.batuku.repository;

import org.example.batuku.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByUserId(Long userId);
    boolean existsByUserIdAndBeatId(Long userId, Long beatId);

    @Query("SELECT COUNT(p) FROM Purchase p")
    long countTotalSales();

    @Query("SELECT p FROM Purchase p WHERE p.beat.producer.id = :userId ORDER BY p.purchasedAt DESC")
    List<Purchase> findByProducerUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(p.price), 0) FROM Purchase p WHERE p.beat.producer.id = :userId")
    java.math.BigDecimal sumRevenueByProducerUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
