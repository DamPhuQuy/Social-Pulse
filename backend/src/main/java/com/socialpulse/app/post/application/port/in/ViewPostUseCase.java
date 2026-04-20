package com.socialpulse.app.post.application.port.in;

import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.post.application.dto.response.ViewPostResponse;

public interface ViewPostUseCase {
	ViewPostResponse viewPost(Long postId, CustomUserDetails currentUser);
}
