package org.example.batuku.repository;

import org.example.batuku.domain.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
