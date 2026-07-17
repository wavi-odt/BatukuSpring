package org.example.batuku.repository;

import org.example.batuku.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTrackIdAndParentCommentIsNull(Long trackId);
    List<Comment> findByParentCommentId(Long parentCommentId);
    long countByTrackId(Long trackId);
}
