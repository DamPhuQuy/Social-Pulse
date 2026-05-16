package com.socialpulse.app.report.application.service;

import org.springframework.stereotype.Service;

import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.ReportCode;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.report.domain.enums.ReportTargetType;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class ReportTargetValidator {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public ReportTargetValidator(
            PostRepository postRepository,
            CommentRepository commentRepository,
            UserRepository userRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    public void validate(ReportTargetType targetType, Long targetId) {
        boolean exists = switch (targetType) {
            case POST -> postExists(targetId);
            case COMMENT -> commentExists(targetId);
            case USER -> userRepository.findById(targetId).isPresent();
        };

        if (!exists) {
            throw new AppException(ReportCode.REPORT_TARGET_NOT_FOUND);
        }
    }

    private boolean postExists(Long targetId) {
        return postRepository.findById(targetId)
                .filter(post -> post.getDeletedAt() == null)
                .isPresent();
    }

    private boolean commentExists(Long targetId) {
        return commentRepository.findById(targetId)
                .filter(comment -> !comment.isDeleted())
                .isPresent();
    }
}
