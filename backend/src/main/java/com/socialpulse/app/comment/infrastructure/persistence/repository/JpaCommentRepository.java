package com.socialpulse.app.comment.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.socialpulse.app.comment.infrastructure.persistence.entity.CommentEntity;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface JpaCommentRepository extends JpaRepository<CommentEntity, Long> {
    @Query("SELECT c FROM CommentEntity c JOIN FETCH c.user WHERE c.post.id = :postId AND c.parentComment IS NULL AND c.deleted = false AND (:lastId = 0L OR c.id < :lastId) ORDER BY c.id DESC")
    List<CommentEntity> findTopLevelCommentsByPostId(@Param("postId") Long postId, @Param("lastId") long lastId, Pageable pageable);

    @Query("SELECT c FROM CommentEntity c JOIN FETCH c.user WHERE c.post.id = :postId AND c.parentComment.id = :parentCommentId AND c.deleted = false AND (:lastId = 0L OR c.id < :lastId) ORDER BY c.id DESC")
    List<CommentEntity> findRepliesByParentCommentId(
            @Param("postId") Long postId,
            @Param("parentCommentId") Long parentCommentId,
            @Param("lastId") long lastId,
            Pageable pageable);

    @Query("SELECT c.parentComment.id, COUNT(c.id) FROM CommentEntity c WHERE c.parentComment.id IN :parentCommentIds AND c.deleted = false GROUP BY c.parentComment.id")
    List<Object[]> countRepliesByParentCommentIds(@Param("parentCommentIds") java.util.Set<Long> parentCommentIds);
}
