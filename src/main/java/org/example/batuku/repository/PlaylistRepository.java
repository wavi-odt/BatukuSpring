package org.example.batuku.repository;

import org.example.batuku.domain.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByUserId(Long userId);
    List<Playlist> findTop5ByTitleContainingIgnoreCase(String title);
}
