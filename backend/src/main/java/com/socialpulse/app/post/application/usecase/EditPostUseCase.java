package com.socialpulse.app.post.application.usecase;

import com.socialpulse.app.post.application.dto.request.PostUpdateRequest;
import com.socialpulse.app.post.application.dto.response.PostUpdateResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface EditPostUseCase {
    PostUpdateResponse editPost(Long postId, PostUpdateRequest request, CustomUserDetails currentUser);
}
