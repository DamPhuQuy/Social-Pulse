package com.socialpulse.app.share.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.share.domain.repository.ShareRepository;
import com.socialpulse.app.share.infrastructure.persistence.repository.JpaShareRepository;

public class ShareRepositoryAdapter implements ShareRepository {
    private final JpaShareRepository jpaShareRepository;

    public ShareRepositoryAdapter(JpaShareRepository jpaShareRepository) {
        this.jpaShareRepository = jpaShareRepository;
    }

    @Override
    public Page<Post> findPostsSharedByUserId(Long userId, Pageable pageable) {
        return jpaShareRepository.findPostsSharedByUserId(userId, pageable);
    }
}
