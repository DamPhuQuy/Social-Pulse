package com.socialpulse.app.share.infrastructure.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.share.infrastructure.persistence.entity.ShareEntity;

public interface JpaShareRepository extends JpaRepository<ShareEntity, Long> {
    Page<Post> findPostsSharedByUserId(Long userId, Pageable pageable);
}
