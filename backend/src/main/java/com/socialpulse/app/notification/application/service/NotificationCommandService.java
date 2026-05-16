package com.socialpulse.app.notification.application.service;

import org.springframework.stereotype.Service;

import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.notification.domain.enums.NotificationResourceType;
import com.socialpulse.app.notification.domain.enums.NotificationType;
import com.socialpulse.app.notification.domain.model.Notification;
import com.socialpulse.app.notification.domain.repository.NotificationRepository;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class NotificationCommandService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationCommandService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void notifyFollow(Long actorId, Long recipientId) {
        create(actorId, recipientId, NotificationType.FOLLOWED_YOU, NotificationResourceType.USER, actorId, "started following you");
    }

    public void notifyPostReaction(Long actorId, Long recipientId, Long postId, ReactionType reactionType) {
        create(actorId, recipientId, NotificationType.POST_REACTED, NotificationResourceType.POST, postId,
                reactedMessage(reactionType, "post"));
    }

    public void notifyCommentOnPost(Long actorId, Long recipientId, Long postId, Long commentId) {
        create(actorId, recipientId, NotificationType.COMMENTED_ON_POST, NotificationResourceType.POST, postId,
                "commented on your post");
    }

    public void notifyReply(Long actorId, Long recipientId, Long commentId) {
        create(actorId, recipientId, NotificationType.REPLIED_TO_COMMENT, NotificationResourceType.COMMENT, commentId,
                "replied to your comment");
    }

    public void notifyCommentReaction(Long actorId, Long recipientId, Long commentId, ReactionType reactionType) {
        create(actorId, recipientId, NotificationType.COMMENT_REACTED, NotificationResourceType.COMMENT, commentId,
                reactedMessage(reactionType, "comment"));
    }

    private void create(
            Long actorId,
            Long recipientId,
            NotificationType type,
            NotificationResourceType resourceType,
            Long resourceId,
            String actionText) {
        if (actorId == null || recipientId == null || actorId.equals(recipientId)) {
            return;
        }

        String actorUsername = userRepository.findById(actorId)
                .map(User::getUsername)
                .orElse("Someone");

        notificationRepository.save(Notification.builder()
                .recipientId(recipientId)
                .actorId(actorId)
                .type(type)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .message(actorUsername + " " + actionText)
                .build());
    }

    private String reactedMessage(ReactionType reactionType, String resourceLabel) {
        if (reactionType == ReactionType.DOWNVOTE) {
            return "downvoted your " + resourceLabel;
        }
        return "upvoted your " + resourceLabel;
    }
}
