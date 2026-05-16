package com.socialpulse.app.comment.application.service;

import java.util.List;

import com.socialpulse.app.comment.application.dto.response.CommentResponse;
import com.socialpulse.app.comment.application.usecase.GetTopLevelCommentsUseCase;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.comment.domain.model.Comment;

public class GetTopLevelCommentsService implements GetTopLevelCommentsUseCase {

    private final CommentRepository commentRepository;
    private final CommentResponseAssembler commentResponseAssembler;

    public GetTopLevelCommentsService(CommentRepository commentRepository,
                                      CommentResponseAssembler commentResponseAssembler) {
        this.commentRepository = commentRepository;
        this.commentResponseAssembler = commentResponseAssembler;
    }

    @Override
    public List<CommentResponse> getTopLevelComments(Long postId, Long lastId, int limit) {
        List<Comment> comments = commentRepository.findTopLevelCommentsByPostId(postId, lastId == null ? 0L : lastId, limit);
        
        if (comments.isEmpty()) {
            return List.of();
        }

        return commentResponseAssembler.toCommentResponses(comments);
    }
}
