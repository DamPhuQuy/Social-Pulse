package com.socialpulse.app.comment.application.service;

import com.socialpulse.app.comment.application.dto.request.CommentUpdateRequest;
import com.socialpulse.app.comment.application.dto.response.CommentCreationResponse;
import com.socialpulse.app.comment.application.usecase.UpdateCommentUseCase;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.CommentCode;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class UpdateCommentService implements UpdateCommentUseCase {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentResponseAssembler commentResponseAssembler;

    public UpdateCommentService(CommentRepository commentRepository,
                                PostRepository postRepository,
                                UserRepository userRepository,
                                CommentResponseAssembler commentResponseAssembler) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentResponseAssembler = commentResponseAssembler;
    }

    @Override
    public CommentCreationResponse updateComment(Long postId, Long commentId, CommentUpdateRequest request, CustomUserDetails currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));
        validatePostAccessible(post, currentUser);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(CommentCode.COMMENT_NOT_FOUND));

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
            throw new AppException(CommentCode.CANNOT_EDIT_DELETED_COMMENT);
        }

        comment.updateContent(request.getContent());

        Comment updatedComment = commentRepository.save(comment);

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        return commentResponseAssembler.toCommentCreationResponse(updatedComment, user);
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
