package org.example.batuku.repository;

import org.example.batuku.domain.UnmappedGenre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnmappedGenreRepository extends JpaRepository<UnmappedGenre, Long> {
    Optional<UnmappedGenre> findByNameIgnoreCase(String name);
    List<UnmappedGenre> findAllByOrderByOccurrencesDescNameAsc();
    long count();
}
