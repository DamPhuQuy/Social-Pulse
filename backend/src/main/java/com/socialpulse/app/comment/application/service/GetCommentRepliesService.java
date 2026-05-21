package com.socialpulse.app.comment.application.service;

import java.util.List;

import com.socialpulse.app.comment.application.dto.response.CommentResponse;
import com.socialpulse.app.comment.application.usecase.GetCommentRepliesUseCase;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.CommentCode;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;

public class GetCommentRepliesService implements GetCommentRepliesUseCase {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentResponseAssembler commentResponseAssembler;

    public GetCommentRepliesService(
            CommentRepository commentRepository,
            PostRepository postRepository,
            CommentResponseAssembler commentResponseAssembler) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.commentResponseAssembler = commentResponseAssembler;
    }

    @Override
    public List<CommentResponse> getReplies(Long postId, Long commentId, Long lastId, int limit, Long viewerUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));
        validatePostAccessible(post, viewerUserId);

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

        return commentResponseAssembler.toCommentResponses(replies, viewerUserId);
    }

    private void validatePostAccessible(Post post, Long viewerUserId) {
        if (post.getDeletedAt() != null) {
            throw new AppException(PostCode.POST_NOT_FOUND);
        }

        if (!post.isPublic() && !post.getUserId().equals(viewerUserId)) {
            throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
        }
    }
}
