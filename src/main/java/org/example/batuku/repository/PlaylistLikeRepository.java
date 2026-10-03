package org.example.batuku.repository;

import org.example.batuku.domain.PlaylistLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistLikeRepository extends JpaRepository<PlaylistLike, Long> {
    boolean existsByUserIdAndPlaylistId(Long userId, Long playlistId);
    long countByPlaylistId(Long playlistId);
    void deleteByUserIdAndPlaylistId(Long userId, Long playlistId);

    @Query(value = """
            SELECT COALESCE(MAX(cnt), 0) FROM (
              SELECT COUNT(pl.id) AS cnt
              FROM playlist_likes pl
              JOIN playlists p ON pl.playlist_id = p.id
              WHERE p.user_id = :userId AND p.is_system_generated = false
              GROUP BY pl.playlist_id
            ) counts
            """, nativeQuery = true)
    long maxLikesByUserPlaylists(@Param("userId") Long userId);
}
