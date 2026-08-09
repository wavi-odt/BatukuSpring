package org.example.batuku.repository;

import org.example.batuku.domain.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByUserIdAndTrackId(Long userId, Long trackId);
    long countByTrackId(Long trackId);
    void deleteByUserIdAndTrackId(Long userId, Long trackId);
    void deleteByTrackId(Long trackId);
    List<Like> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query(value = "SELECT COUNT(*) FROM likes l INNER JOIN tracks t ON l.track_id = t.id " +
                   "WHERE t.artist_profile_id = :artistId AND l.created_at >= :since", nativeQuery = true)
    long countByTrackArtistProfileIdAndCreatedAtAfter(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);

    @Query(value = "SELECT COUNT(*) FROM likes l INNER JOIN tracks t ON l.track_id = t.id " +
                   "WHERE t.artist_profile_id = :artistId AND l.created_at BETWEEN :from AND :to", nativeQuery = true)
    long countByTrackArtistProfileIdAndCreatedAtBetween(@Param("artistId") Long artistId,
                                                         @Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to);

    @Query(value = "SELECT DATE(l.created_at), COUNT(*) FROM likes l " +
                   "INNER JOIN tracks t ON l.track_id = t.id " +
                   "WHERE t.artist_profile_id = :artistId AND l.created_at >= :since " +
                   "GROUP BY DATE(l.created_at) ORDER BY DATE(l.created_at)", nativeQuery = true)
    List<Object[]> findDailyLikesByArtist(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);
}
