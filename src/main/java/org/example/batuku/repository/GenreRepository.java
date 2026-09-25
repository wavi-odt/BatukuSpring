package org.example.batuku.repository;

import org.example.batuku.domain.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    boolean existsByName(String name);
    Optional<Genre> findByNameIgnoreCase(String name);
    List<Genre> findAllByCaboverdeanOrderByNameAsc(boolean caboverdean);

    @Query("SELECT g.id, COUNT(t) FROM Track t JOIN t.genre g GROUP BY g.id")
    List<Object[]> countTracksByGenre();
}
