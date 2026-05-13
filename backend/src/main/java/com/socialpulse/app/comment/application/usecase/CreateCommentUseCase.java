package com.socialpulse.app.comment.application.usecase;

import com.socialpulse.app.comment.application.dto.request.CommentCreationRequest;
import com.socialpulse.app.comment.application.dto.response.CommentCreationResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface CreateCommentUseCase {
    CommentCreationResponse createComment(Long postId, CommentCreationRequest request, CustomUserDetails currentUser);
}

