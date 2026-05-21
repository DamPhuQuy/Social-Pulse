package com.socialpulse.app.comment.application.usecase;

import java.util.List;
import com.socialpulse.app.comment.application.dto.response.CommentResponse;

public interface GetTopLevelCommentsUseCase {
    List<CommentResponse> getTopLevelComments(Long postId, Long lastId, int limit, Long viewerUserId);
}
