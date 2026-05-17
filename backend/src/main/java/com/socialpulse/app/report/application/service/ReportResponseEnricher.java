package com.socialpulse.app.report.application.service;

import java.util.List;
import java.util.stream.Collectors;

import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.report.application.dto.response.ReportResponse;
import com.socialpulse.app.report.domain.enums.ReportTargetType;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Enriches ReportResponse DTOs with the actual content of the reported target
 * (post body, comment text, or user info) so admins can review without extra API calls.
 */
@Slf4j
public class ReportResponseEnricher {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public ReportResponseEnricher(PostRepository postRepository,
                                  CommentRepository commentRepository,
                                  UserRepository userRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    public ReportResponse enrich(ReportResponse response) {
        if (response == null) {
            return null;
        }

        try {
            return switch (response.getTargetType()) {
                case POST -> enrichWithPost(response);
                case COMMENT -> enrichWithComment(response);
                case USER -> enrichWithUser(response);
            };
        } catch (Exception e) {
            log.warn("Failed to enrich report {} with target content: {}",
                    response.getId(), e.getMessage());
            return response;
        }
    }

    public List<ReportResponse> enrich(List<ReportResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return responses;
        }
        return responses.stream()
                .map(this::enrich)
                .collect(Collectors.toList());
    }

    private ReportResponse enrichWithPost(ReportResponse response) {
        return postRepository.findById(response.getTargetId())
                .map(post -> {
                    ReportResponse.ReportResponseBuilder builder = response.toBuilder()
                            .targetContent(post.getContent())
                            .targetOwnerId(post.getUserId());
                    resolveUsername(post.getUserId(), builder);
                    return builder.build();
                })
                .orElseGet(() -> response.toBuilder()
                        .targetContent("[Deleted Post]")
                        .build());
    }

    private ReportResponse enrichWithComment(ReportResponse response) {
        return commentRepository.findById(response.getTargetId())
                .map(comment -> {
                    String content = comment.isDeleted()
                            ? "[Deleted Comment]"
                            : comment.getContent();
                    ReportResponse.ReportResponseBuilder builder = response.toBuilder()
                            .targetContent(content)
                            .targetOwnerId(comment.getUserId());
                    resolveUsername(comment.getUserId(), builder);
                    return builder.build();
                })
                .orElseGet(() -> response.toBuilder()
                        .targetContent("[Deleted Comment]")
                        .build());
    }

    private ReportResponse enrichWithUser(ReportResponse response) {
        return userRepository.findById(response.getTargetId())
                .map(user -> response.toBuilder()
                        .targetContent(user.getUsername())
                        .targetOwnerId(user.getId())
                        .targetOwnerUsername(user.getUsername())
                        .build())
                .orElseGet(() -> response.toBuilder()
                        .targetContent("[Deleted User]")
                        .build());
    }

    private void resolveUsername(Long userId, ReportResponse.ReportResponseBuilder builder) {
        userRepository.findById(userId)
                .ifPresent(user -> builder.targetOwnerUsername(user.getUsername()));
    }
}
