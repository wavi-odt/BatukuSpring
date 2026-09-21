package org.example.batuku.repository;

import org.example.batuku.domain.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByUserId(Long userId);
    List<Playlist> findTop5ByTitleContainingIgnoreCase(String title);
    Optional<Playlist> findByUserIdAndIsSystemGeneratedTrue(Long userId);
    List<Playlist> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Playlist> findByUserIdAndIsPublicTrueAndIsSystemGeneratedFalseOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndIsSystemGeneratedFalse(Long userId);
}
