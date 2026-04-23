package com.socialpulse.app.post.application.usecase;

import com.socialpulse.app.security.user.CustomUserDetails;

public interface DeletePostUseCase {
    void deletePost(Long postId, CustomUserDetails currentUser);
}
