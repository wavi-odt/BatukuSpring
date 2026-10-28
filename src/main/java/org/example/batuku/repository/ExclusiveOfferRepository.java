package org.example.batuku.repository;

import org.example.batuku.domain.ExclusiveOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ExclusiveOfferRepository extends JpaRepository<ExclusiveOffer, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM ExclusiveOffer o WHERE o.id = :id")
    Optional<ExclusiveOffer> findByIdForUpdate(@Param("id") Long id);

    List<ExclusiveOffer> findByFanIdOrderByCreatedAtDesc(Long fanId);

    @Query("SELECT o FROM ExclusiveOffer o WHERE o.beat.producer.id = :producerId ORDER BY o.createdAt DESC")
    List<ExclusiveOffer> findByProducerIdOrderByCreatedAtDesc(
            @org.springframework.data.repository.query.Param("producerId") Long producerId);

    boolean existsByFanIdAndBeatIdAndStatus(Long fanId, Long beatId, String status);

    List<ExclusiveOffer> findByBeatIdAndStatus(Long beatId, String status);
}
