package org.example.batuku.repository;

import org.example.batuku.domain.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    boolean existsByName(String name);
}
