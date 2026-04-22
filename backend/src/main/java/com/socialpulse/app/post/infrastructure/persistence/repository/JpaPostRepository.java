package com.socialpulse.app.post.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.infrastructure.persistence.entity.PostEntity;

@Repository
public interface JpaPostRepository extends JpaRepository<PostEntity, Long> {

    // get posts by user id
    Page<PostEntity> findByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndParentPostIdAndType(Long userId, Long parentPostId, PostType type);
}
