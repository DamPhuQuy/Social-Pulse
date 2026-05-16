package com.socialpulse.app.comment.application.service;

import java.util.List;

import com.socialpulse.app.comment.application.dto.response.CommentResponse;
import com.socialpulse.app.comment.application.usecase.GetCommentRepliesUseCase;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.CommentCode;

public class GetCommentRepliesService implements GetCommentRepliesUseCase {
    private final CommentRepository commentRepository;
    private final CommentResponseAssembler commentResponseAssembler;

    public GetCommentRepliesService(
            CommentRepository commentRepository,
            CommentResponseAssembler commentResponseAssembler) {
        this.commentRepository = commentRepository;
        this.commentResponseAssembler = commentResponseAssembler;
    }

    @Override
    public List<CommentResponse> getReplies(Long postId, Long commentId, Long lastId, int limit) {
        Comment parentComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(CommentCode.COMMENT_NOT_FOUND));

        if (!parentComment.getPostId().equals(postId)) {
            throw new AppException(CommentCode.COMMENT_NOT_BELONG_TO_POST);
        }

        List<Comment> replies = commentRepository.findRepliesByParentCommentId(
                postId,
                commentId,
                lastId == null ? 0L : lastId,
                limit);

        return commentResponseAssembler.toCommentResponses(replies);
    }
}
