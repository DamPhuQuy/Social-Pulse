package com.socialpulse.app.post.application.port.out;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.post.domain.model.Post;

public interface PostRepositoryPort {
    Optional<Post> findById(Long id);

    Post save(Post post);

    Page<Post> findByUserId(Long userId, Pageable pageable);
}
