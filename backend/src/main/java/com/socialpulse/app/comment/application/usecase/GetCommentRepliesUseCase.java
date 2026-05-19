package com.socialpulse.app.comment.application.usecase;

import java.util.List;

import com.socialpulse.app.comment.application.dto.response.CommentResponse;

public interface GetCommentRepliesUseCase {
    List<CommentResponse> getReplies(Long postId, Long commentId, Long lastId, int limit, Long viewerUserId);
}
