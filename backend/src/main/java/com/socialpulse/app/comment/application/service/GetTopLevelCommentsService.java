package com.socialpulse.app.comment.application.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.socialpulse.app.comment.application.dto.mapper.CommentMapper;
import com.socialpulse.app.comment.application.dto.response.CommentResponse;
import com.socialpulse.app.comment.application.usecase.GetTopLevelCommentsUseCase;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.user.domain.repository.UserRepository;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;

public class GetTopLevelCommentsService implements GetTopLevelCommentsUseCase {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    public GetTopLevelCommentsService(CommentRepository commentRepository,
                                      UserRepository userRepository,
                                      CommentMapper commentMapper) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.commentMapper = commentMapper;
    }

    @Override
    public List<CommentResponse> getTopLevelComments(Long postId, Long lastId, int limit) {
        List<Comment> comments = commentRepository.findTopLevelCommentsByPostId(postId, lastId == null ? 0L : lastId, limit);
        
        if (comments.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());

        java.util.Map<Long, User> userMap = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return comments.stream().map(comment -> {
            User user = userMap.get(comment.getUserId());
            if (user == null) {
                throw new AppException(UserCode.USER_NOT_FOUND);
            }
            return commentMapper.toCommentResponse(comment, user);
        }).collect(Collectors.toList());
    }
}
