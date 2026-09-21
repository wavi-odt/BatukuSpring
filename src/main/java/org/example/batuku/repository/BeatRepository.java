package org.example.batuku.repository;

import org.example.batuku.domain.Beat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BeatRepository extends JpaRepository<Beat, Long> {
    List<Beat> findByGenre(String genre);
    List<Beat> findByProducerIdOrderByCreatedAtDesc(Long producerId);

    @Query("SELECT b FROM Beat b WHERE b.isFeatured = true AND b.soldExclusively = false ORDER BY b.plays DESC")
    List<Beat> findFeaturedOrdered(Pageable pageable);

    @Query("SELECT b FROM Beat b WHERE b.soldExclusively = false ORDER BY b.plays DESC")
    List<Beat> findByPlaysDesc(Pageable pageable);

    @Query("SELECT DISTINCT b.genre FROM Beat b WHERE b.genre IS NOT NULL ORDER BY b.genre")
    List<String> findDistinctGenres();

    @Query("SELECT COUNT(DISTINCT b.producer.id) FROM Beat b")
    long countDistinctProducers();
}
