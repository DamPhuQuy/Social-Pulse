package com.socialpulse.app.post.application.usecase;

import com.socialpulse.app.post.application.dto.response.ViewPostResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface ViewPostUseCase {
	ViewPostResponse viewPost(Long postId, CustomUserDetails currentUser);
}

