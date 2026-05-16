package com.socialpulse.app.comment.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.socialpulse.app.comment.application.dto.mapper.CommentMapper;
import com.socialpulse.app.comment.application.dto.response.CommentCreationResponse;
import com.socialpulse.app.comment.application.dto.response.CommentResponse;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class CommentResponseAssembler {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    public CommentResponseAssembler(
            CommentRepository commentRepository,
            UserRepository userRepository,
            CommentMapper commentMapper) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.commentMapper = commentMapper;
    }

    public CommentCreationResponse toCommentCreationResponse(Comment comment, User user) {
        return commentMapper.toCommentCreationResponse(comment, user, countReplies(comment.getId()));
    }

    public List<CommentResponse> toCommentResponses(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Set<Long> commentIds = comments.stream()
                .map(Comment::getId)
                .collect(Collectors.toSet());
        Map<Long, Long> replyCounts = commentRepository.countRepliesByParentCommentIds(commentIds);

        return comments.stream()
                .map(comment -> toCommentResponse(comment, userMap, replyCounts))
                .toList();
    }

    private CommentResponse toCommentResponse(
            Comment comment,
            Map<Long, User> userMap,
            Map<Long, Long> replyCounts) {
        User user = userMap.get(comment.getUserId());
        if (user == null) {
            throw new AppException(UserCode.USER_NOT_FOUND);
        }

        return commentMapper.toCommentResponse(comment, user, toInt(replyCounts.get(comment.getId())));
    }

    private int countReplies(Long commentId) {
        return toInt(commentRepository.countRepliesByParentCommentIds(Set.of(commentId)).get(commentId));
    }

    private int toInt(Long value) {
        if (value == null) {
            return 0;
        }
        return Math.toIntExact(value);
    }
}
