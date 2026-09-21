package org.example.batuku.repository;

import org.example.batuku.domain.Comment;
import org.example.batuku.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTrackIdAndParentCommentIsNull(Long trackId);
    List<Comment> findByAlbumIdAndParentCommentIsNull(Long albumId);
    List<Comment> findByParentCommentId(Long parentCommentId);
    long countByTrackId(Long trackId);
    long countByAlbumId(Long albumId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.track.artistProfile.id = :profileId AND c.parentComment IS NULL AND c.createdAt > :since")
    long countByArtistProfileAndCreatedAtAfter(@Param("profileId") Long profileId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.track.artistProfile.id = :profileId AND c.parentComment IS NULL AND c.createdAt BETWEEN :from AND :to")
    long countByArtistProfileAndCreatedAtBetween(@Param("profileId") Long profileId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT c FROM Comment c WHERE c.track.artistProfile.user = :user AND c.parentComment IS NULL ORDER BY c.createdAt DESC")
    List<Comment> findByArtistUser(@Param("user") User user);

    @Query("SELECT c FROM Comment c WHERE c.parentComment.id IN :parentIds ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    long countByUserId(Long userId);
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);

    @Modifying
    @Query(value = "DELETE c FROM comments c JOIN comments parent ON c.parent_comment_id = parent.id WHERE parent.track_id = :trackId", nativeQuery = true)
    void deleteRepliesByParentTrackId(@Param("trackId") Long trackId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.track.id = :trackId")
    void deleteByTrackId(@Param("trackId") Long trackId);

    @Modifying
    @Query(value = "DELETE c FROM comments c JOIN comments parent ON c.parent_comment_id = parent.id WHERE parent.album_id = :albumId", nativeQuery = true)
    void deleteRepliesByParentAlbumId(@Param("albumId") Long albumId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.album.id = :albumId")
    void deleteByAlbumId(@Param("albumId") Long albumId);
}
