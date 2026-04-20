package com.socialpulse.app.post.application.port.in;

import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.post.application.dto.request.PostCreationRequest;
import com.socialpulse.app.post.application.dto.response.PostCreationResponse;

public interface CreatePostUseCase {
	PostCreationResponse createPost(PostCreationRequest request, CustomUserDetails currentUser);
}
