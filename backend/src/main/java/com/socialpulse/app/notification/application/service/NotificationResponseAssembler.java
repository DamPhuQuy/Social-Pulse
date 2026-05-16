package com.socialpulse.app.notification.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.notification.application.dto.response.NotificationResponse;
import com.socialpulse.app.notification.domain.model.Notification;
import com.socialpulse.app.user.application.dto.mapper.UserMapper;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class NotificationResponseAssembler {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public NotificationResponseAssembler(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public List<NotificationResponse> assemble(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return List.of();
        }

        Set<Long> actorIds = notifications.stream()
                .map(Notification::getActorId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userRepository.findByIds(actorIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return notifications.stream()
                .map(notification -> toResponse(notification, userMap.get(notification.getActorId())))
                .toList();
    }

    private NotificationResponse toResponse(Notification notification, User actor) {
        if (actor == null) {
            throw new AppException(UserCode.USER_NOT_FOUND);
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .actor(userMapper.toUserSummary(actor))
                .recipientId(notification.getRecipientId())
                .type(notification.getType())
                .resourceType(notification.getResourceType())
                .resourceId(notification.getResourceId())
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
