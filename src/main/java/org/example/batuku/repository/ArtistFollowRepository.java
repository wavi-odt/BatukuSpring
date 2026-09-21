package org.example.batuku.repository;

import org.example.batuku.domain.ArtistFollow;
import org.example.batuku.dto.FanProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ArtistFollowRepository extends JpaRepository<ArtistFollow, Long> {
    boolean existsByFollowerIdAndArtistProfileId(Long followerId, Long artistProfileId);
    void deleteByFollowerIdAndArtistProfileId(Long followerId, Long artistProfileId);
    long countByArtistProfileId(Long artistProfileId);
    List<ArtistFollow> findByFollowerIdOrderByCreatedAtDesc(Long followerId);
    List<ArtistFollow> findByArtistProfileIdOrderByCreatedAtDesc(Long artistProfileId);
    long countByFollowerId(Long followerId);
    long countByFollowerIdAndCreatedAtAfter(Long followerId, LocalDateTime since);
    long countByArtistProfileIdAndCreatedAtAfter(Long artistProfileId, LocalDateTime since);
    long countByArtistProfileIdAndCreatedAtBetween(Long artistProfileId, LocalDateTime from, LocalDateTime to);

    @Query(nativeQuery = true, value = """
            SELECT
                u.id                                    AS id,
                u.name                                  AS name,
                u.username                              AS username,
                u.avatar_url                            AS avatarUrl,
                COALESCE(u.location, u.country)         AS location,
                af.created_at                           AS followedAt,
                MAX(p.played_at)                        AS lastPlayedAt,
                COUNT(DISTINCT p.id)                    AS plays,
                COUNT(DISTINCT l.id)                    AS likes,
                COUNT(DISTINCT c.id)                    AS comments
            FROM artist_follows af
            JOIN users u ON af.follower_id = u.id
            LEFT JOIN plays p
                ON p.user_id = u.id
                AND p.track_id IN (SELECT t.id FROM tracks t WHERE t.artist_profile_id = :artistProfileId)
            LEFT JOIN likes l
                ON l.user_id = u.id
                AND l.track_id IN (SELECT t.id FROM tracks t WHERE t.artist_profile_id = :artistProfileId)
            LEFT JOIN comments c
                ON c.user_id = u.id
                AND c.track_id IN (SELECT t.id FROM tracks t WHERE t.artist_profile_id = :artistProfileId)
                AND c.parent_comment_id IS NULL
            WHERE af.artist_profile_id = :artistProfileId
            GROUP BY u.id, u.name, u.username, u.avatar_url, u.location, u.country, af.created_at
            ORDER BY af.created_at DESC
            """)
    List<FanProjection> findFansWithStatsByArtistProfile(@Param("artistProfileId") Long artistProfileId);

    @Query(value = "SELECT DATE(created_at), COUNT(*) FROM artist_follows " +
                   "WHERE artist_profile_id = :artistId AND created_at >= :since " +
                   "GROUP BY DATE(created_at) ORDER BY DATE(created_at)", nativeQuery = true)
    List<Object[]> findDailyFollowsByArtist(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);
}
