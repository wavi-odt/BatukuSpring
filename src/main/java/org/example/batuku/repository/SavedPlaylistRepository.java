package org.example.batuku.repository;

import org.example.batuku.domain.SavedPlaylist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SavedPlaylistRepository extends JpaRepository<SavedPlaylist, Long> {
    boolean existsByUserIdAndPlaylistId(Long userId, Long playlistId);
    Optional<SavedPlaylist> findByUserIdAndPlaylistId(Long userId, Long playlistId);
    List<SavedPlaylist> findByUserIdOrderBySavedAtDesc(Long userId);
}
