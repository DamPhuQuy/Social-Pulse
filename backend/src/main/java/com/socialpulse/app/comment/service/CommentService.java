package com.socialpulse.app.comment.service;

import org.springframework.stereotype.Service;

import com.socialpulse.app.comment.dto.request.CommentCreationRequest;
import com.socialpulse.app.comment.dto.response.CommentCreationResponse;
import com.socialpulse.app.comment.entity.Comment;
import com.socialpulse.app.comment.repository.CommentRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.status.CommentCode;
import com.socialpulse.app.common.status.PostCode;
import com.socialpulse.app.common.status.UserCode;
import com.socialpulse.app.post.entity.Post;
import com.socialpulse.app.post.repository.PostRepository;
import com.socialpulse.app.user.dto.response.UserSummary;
import com.socialpulse.app.user.entity.User;
import com.socialpulse.app.user.repository.UserRepository;

@Service
public class CommentService {
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public CommentCreationResponse createComment(CommentCreationRequest request, Long userId) {
        Post post = postRepository.findById(request.getPostId())
            .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        Comment parent = null;

        if (request.getParentCommentId() != null) {
            parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new AppException(CommentCode.COMMENT_NOT_FOUND));


            // do not reply to a comment that is already a reply
            if (parent.getParentComment() != null) {
                throw new AppException(CommentCode.REPLY_TO_COMMENT_NOT_ALLOWED);
            }

            // parent comment must belong to the same post
            if (!parent.getPost().getId().equals(post.getId())) {
                throw new AppException(CommentCode.PARENT_MUST_BELONG_TO_SAME_POST);
            }

            // cannot reply to a deleted comment
            if (parent.isDeleted()) {
                throw new AppException(CommentCode.CANNOT_REPLY_TO_DELETED_COMMENT);
            }
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .post(post)
                .parentComment(parent)
                .user(user)
                .build();

        comment = commentRepository.save(comment);

        return CommentCreationResponse.builder()
                .id(comment.getId())
                .postId(post.getId())
                .user(UserSummary.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .build())
                .parentCommentId(parent != null ? parent.getId() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .replyCount(0)
                .build();
    }
}
