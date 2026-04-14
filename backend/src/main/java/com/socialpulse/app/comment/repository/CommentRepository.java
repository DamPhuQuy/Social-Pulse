package com.socialpulse.app.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.comment.entity.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

}
