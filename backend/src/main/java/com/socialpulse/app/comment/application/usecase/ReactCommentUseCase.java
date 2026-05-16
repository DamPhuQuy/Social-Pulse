package com.socialpulse.app.comment.application.usecase;

import com.socialpulse.app.comment.application.dto.request.CommentReactionRequest;
import com.socialpulse.app.comment.application.dto.response.CommentReactionResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface ReactCommentUseCase {
    CommentReactionResponse react(Long postId, Long commentId, CommentReactionRequest request, CustomUserDetails currentUser);
}
