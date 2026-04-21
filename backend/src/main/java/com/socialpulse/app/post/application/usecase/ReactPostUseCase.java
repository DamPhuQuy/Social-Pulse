package com.socialpulse.app.post.application.usecase;

import com.socialpulse.app.post.application.dto.request.PostReactionRequest;
import com.socialpulse.app.post.application.dto.response.PostReactionResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface ReactPostUseCase {
	PostReactionResponse react(PostReactionRequest request, CustomUserDetails currentUser);
}

