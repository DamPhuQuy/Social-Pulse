package com.socialpulse.app.comment.application.service;

import com.socialpulse.app.comment.application.usecase.DeleteCommentUseCase;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.CommentCode;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;
import org.springframework.transaction.annotation.Transactional;

public class DeleteCommentService implements DeleteCommentUseCase {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public DeleteCommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    @Override
    @Transactional
    public void deleteComment(Long postId, Long commentId, CustomUserDetails currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(CommentCode.COMMENT_NOT_FOUND));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));
        validatePostAccessible(post, currentUser);

        if (!comment.getPostId().equals(postId)) {
            throw new AppException(CommentCode.COMMENT_NOT_BELONG_TO_POST);
        }

        boolean isOwner = comment.getUserId().equals(currentUser.getId());
        boolean hasManagePermission = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("comment:manage"));

        if (!isOwner && !hasManagePermission) {
            throw new AppException(CommentCode.COMMENT_NOT_OWNER);
        }

        if (comment.isDeleted()) {
            throw new AppException(CommentCode.COMMENT_ALREADY_DELETED);
        }

        comment.markDeleted();
        commentRepository.save(comment);
        post.decrementCommentCount();
        postRepository.save(post);
    }

    private void validatePostAccessible(Post post, CustomUserDetails currentUser) {
        if (post.getDeletedAt() != null) {
            throw new AppException(PostCode.POST_NOT_FOUND);
        }

        boolean canAccess = post.isPublic()
                || post.getUserId().equals(currentUser.getId())
                || currentUser.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("post:manage"));

        if (!canAccess) {
            throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
        }
    }
}
