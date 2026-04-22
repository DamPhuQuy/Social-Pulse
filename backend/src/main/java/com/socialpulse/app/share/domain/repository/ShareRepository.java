package com.socialpulse.app.share.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.post.domain.model.Post;

public interface ShareRepository {
    Page<Post> findPostsSharedByUserId(Long userId, Pageable pageable);
}
