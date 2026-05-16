package com.socialpulse.app.comment.application.usecase;

import com.socialpulse.app.comment.application.dto.request.CommentUpdateRequest;
import com.socialpulse.app.comment.application.dto.response.CommentCreationResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface UpdateCommentUseCase {
    CommentCreationResponse updateComment(Long postId, Long commentId, CommentUpdateRequest request, CustomUserDetails currentUser);
}
