package com.socialpulse.app.post.application.port.in;

import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.post.application.dto.request.PostReactionRequest;
import com.socialpulse.app.post.application.dto.response.PostReactionResponse;

public interface ReactPostUseCase {
	PostReactionResponse react(PostReactionRequest request, CustomUserDetails currentUser);
}
