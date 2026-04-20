package com.socialpulse.app.comment.application.port.in;

import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.comment.application.dto.request.CommentCreationRequest;
import com.socialpulse.app.comment.application.dto.response.CommentCreationResponse;

public interface CreateCommentUseCase {
    CommentCreationResponse createComment(CommentCreationRequest request, CustomUserDetails currentUser);
}
