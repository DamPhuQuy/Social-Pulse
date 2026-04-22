package com.socialpulse.app.comment.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.socialpulse.app.comment.infrastructure.persistence.entity.CommentEntity;

public interface JpaCommentRepository extends JpaRepository<CommentEntity, Long> {

}
