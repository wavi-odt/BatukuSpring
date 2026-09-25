package org.example.batuku.repository;

import org.example.batuku.domain.Play;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

public interface PlayRepository extends JpaRepository<Play, Long> {
    long countByTrackId(Long trackId);
    void deleteByTrackId(Long trackId);

    long countByTrackArtistProfileIdAndPlayedAtAfter(Long artistProfileId, LocalDateTime since);
    long countByTrackArtistProfileIdAndPlayedAtBetween(Long artistProfileId, LocalDateTime from, LocalDateTime to);

    @Query(value = "SELECT DATE(p.played_at), COUNT(*) FROM plays p " +
                   "INNER JOIN tracks t ON p.track_id = t.id " +
                   "WHERE t.artist_profile_id = :artistId AND p.played_at >= :since " +
                   "GROUP BY DATE(p.played_at) ORDER BY DATE(p.played_at)", nativeQuery = true)
    List<Object[]> findDailyPlaysByArtist(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);

    @Query(value = "SELECT t.id, t.title, t.cover_url, COUNT(p.id) " +
                   "FROM plays p INNER JOIN tracks t ON p.track_id = t.id " +
                   "WHERE t.artist_profile_id = :artistId AND p.played_at >= :since " +
                   "GROUP BY t.id, t.title, t.cover_url ORDER BY COUNT(p.id) DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findTopTracksByArtist(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);

    @Query(value = "SELECT COALESCE(p.referrer_url, 'direct'), COUNT(*) FROM plays p " +
                   "INNER JOIN tracks t ON p.track_id = t.id " +
                   "WHERE t.artist_profile_id = :artistId AND p.played_at >= :since " +
                   "GROUP BY p.referrer_url ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> findSourceBreakdownByArtist(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);

    @Query(value = "SELECT COUNT(DISTINCT p.user_id) FROM plays p " +
                   "INNER JOIN tracks t ON p.track_id = t.id " +
                   "WHERE t.artist_profile_id = :artistProfileId AND p.played_at >= :since",
           nativeQuery = true)
    long countDistinctListenersByArtistSince(@Param("artistProfileId") Long artistProfileId, @Param("since") LocalDateTime since);

    /* ── Queries de gamificação ────────────────────────────────────── */

    @Query(value = "SELECT COALESCE(SUM(p.duration_played), 0) FROM plays p " +
                   "WHERE p.user_id = :userId AND p.played_at >= :since AND p.duration_played IS NOT NULL",
           nativeQuery = true)
    long sumDurationPlayedSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query(value = "SELECT COUNT(DISTINCT t.artist_profile_id) FROM plays p " +
                   "JOIN tracks t ON p.track_id = t.id " +
                   "WHERE p.user_id = :userId AND p.played_at >= :since",
           nativeQuery = true)
    long countDistinctArtistsSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query(value = "SELECT COUNT(DISTINCT p.track_id) FROM plays p WHERE p.user_id = :userId",
           nativeQuery = true)
    long countDistinctTracksByUser(@Param("userId") Long userId);

    @Query(value = "SELECT COUNT(DISTINCT t.artist_profile_id) FROM plays p " +
                   "JOIN tracks t ON p.track_id = t.id WHERE p.user_id = :userId",
           nativeQuery = true)
    long countDistinctArtistsByUser(@Param("userId") Long userId);

    @Query(value = "SELECT DISTINCT DATE(p.played_at) FROM plays p " +
                   "WHERE p.user_id = :userId AND p.played_at >= :since " +
                   "ORDER BY DATE(p.played_at) DESC",
           nativeQuery = true)
    List<Date> findDistinctPlayDatesSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query(value = "SELECT COALESCE(SUM(p.duration_played), 0) FROM plays p " +
                   "WHERE p.user_id = :userId AND p.duration_played IS NOT NULL",
           nativeQuery = true)
    long sumTotalDurationByUser(@Param("userId") Long userId);

    /* ─────────────────────────────────────────────────────────────── */

    @Query(value = "SELECT p.country, COUNT(*) FROM plays p " +
                   "INNER JOIN tracks t ON p.track_id = t.id " +
                   "WHERE t.artist_profile_id = :artistId AND p.played_at >= :since " +
                   "AND p.country IS NOT NULL AND p.country <> '' " +
                   "GROUP BY p.country ORDER BY COUNT(*) DESC LIMIT 6", nativeQuery = true)
    List<Object[]> findCountryBreakdownByArtist(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);

    @Query(value = "SELECT AVG(CAST(p.duration_played AS FLOAT) / t.duration_ms * 100) " +
                   "FROM plays p INNER JOIN tracks t ON p.track_id = t.id " +
                   "WHERE t.artist_profile_id = :artistId AND p.played_at >= :since " +
                   "AND p.duration_played IS NOT NULL AND t.duration_ms > 0", nativeQuery = true)
    Double findAvgCompletionRateByArtist(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);

    @Query(value = "SELECT g.name, COUNT(p.id) AS plays " +
                   "FROM plays p " +
                   "INNER JOIN tracks t ON p.track_id = t.id " +
                   "INNER JOIN genres g ON t.genre_id = g.id " +
                   "WHERE p.user_id = :userId " +
                   "GROUP BY g.name ORDER BY plays DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findTopGenresByUser(@Param("userId") Long userId);

    @Query(value = "SELECT p.track_id, t.title, t.cover_url, ap.name, ap.id, t.audio_url, t.source, t.spotify_url, MAX(p.played_at) AS last_played " +
                   "FROM plays p " +
                   "INNER JOIN tracks t ON p.track_id = t.id " +
                   "INNER JOIN artist_profiles ap ON t.artist_profile_id = ap.id " +
                   "WHERE p.user_id = :userId " +
                   "GROUP BY p.track_id, t.title, t.cover_url, ap.name, ap.id, t.audio_url, t.source, t.spotify_url " +
                   "ORDER BY last_played DESC LIMIT 10", nativeQuery = true)
    List<Object[]> findRecentlyPlayedByUser(@Param("userId") Long userId);
}
