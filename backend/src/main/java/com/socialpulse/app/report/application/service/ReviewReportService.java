package com.socialpulse.app.report.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.common.exception.AppException;

import com.socialpulse.app.common.exception.status.ReportCode;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.report.application.dto.mapper.ReportMapper;
import com.socialpulse.app.report.application.dto.request.ReviewReportRequest;
import com.socialpulse.app.report.application.dto.response.ReportResponse;
import com.socialpulse.app.report.application.usecase.ReviewReportUseCase;
import com.socialpulse.app.report.domain.enums.ReportStatus;

import com.socialpulse.app.report.domain.model.Report;
import com.socialpulse.app.report.domain.repository.ReportRepository;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReviewReportService implements ReviewReportUseCase {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ReportMapper reportMapper;
    private final ReportResponseEnricher reportResponseEnricher;

    public ReviewReportService(ReportRepository reportRepository,
                               PostRepository postRepository,
                               CommentRepository commentRepository,
                               UserRepository userRepository,
                               ReportMapper reportMapper,
                               ReportResponseEnricher reportResponseEnricher) {
        this.reportRepository = reportRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.reportMapper = reportMapper;
        this.reportResponseEnricher = reportResponseEnricher;
    }

    @Override
    @Transactional
    public ReportResponse reviewReport(Long reportId, ReviewReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ReportCode.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new AppException(ReportCode.REPORT_STATUS_UPDATE_NOT_ALLOWED);
        }

        log.info("Admin reviewing report {} with action {}", reportId, request.getAction());

        switch (request.getAction()) {
            case REJECT -> handleReject(report);
            case DELETE_CONTENT -> handleDeleteContent(report);
            case BAN_USER -> handleBanUser(report);
            case DELETE_CONTENT_AND_BAN_USER -> {
                handleDeleteContent(report);
                handleBanUser(report);
            }
        }

        Report savedReport = reportRepository.save(report);
        ReportResponse response = reportMapper.toResponse(savedReport);
        return reportResponseEnricher.enrich(response);
    }

    private void handleReject(Report report) {
        report.markRejected();
        log.info("Report {} rejected", report.getId());
    }

    private void handleDeleteContent(Report report) {
        switch (report.getTargetType()) {
            case POST -> deletePost(report.getTargetId());
            case COMMENT -> deleteComment(report.getTargetId());
            case USER -> log.info("Target type is USER — skipping content deletion for report {}",
                    report.getId());
        }
        report.markResolved();
    }

    private void handleBanUser(Report report) {
        Long userId = resolveTargetOwnerId(report);
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElse(null);
            if (user != null) {
                user.lockAccount();
                userRepository.save(user);
                log.info("User {} has been banned (account locked) via report {}",
                        userId, report.getId());
            }
        }
        report.markResolved();
    }

    private void deletePost(Long postId) {
        postRepository.findById(postId).ifPresent(post -> {
            if (post.getDeletedAt() == null) {
                postRepository.deleteById(postId);
                log.info("Post {} soft-deleted via report moderation", postId);
            }
        });
    }

    private void deleteComment(Long commentId) {
        commentRepository.findById(commentId).ifPresent(comment -> {
            if (!comment.isDeleted()) {
                comment.markDeleted();
                commentRepository.save(comment);
                log.info("Comment {} marked as deleted via report moderation", commentId);

                // Decrement the comment count on the parent post
                postRepository.findById(comment.getPostId()).ifPresent(post -> {
                    post.decrementCommentCount();
                    postRepository.save(post);
                });
            }
        });
    }

    /**
     * Resolves the owner/author userId of the reported target.
     * For POST/COMMENT, this is the author. For USER, it is the target user itself.
     */
    private Long resolveTargetOwnerId(Report report) {
        return switch (report.getTargetType()) {
            case POST -> postRepository.findById(report.getTargetId())
                    .map(Post::getUserId)
                    .orElse(null);
            case COMMENT -> commentRepository.findById(report.getTargetId())
                    .map(Comment::getUserId)
                    .orElse(null);
            case USER -> report.getTargetId();
        };
    }
}
