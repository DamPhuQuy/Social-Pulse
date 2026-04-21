package com.socialpulse.app.post.application.usecase;

import com.socialpulse.app.post.application.dto.request.PostCreationRequest;
import com.socialpulse.app.post.application.dto.response.PostCreationResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface CreatePostUseCase {
	PostCreationResponse createPost(PostCreationRequest request, CustomUserDetails currentUser);
}

