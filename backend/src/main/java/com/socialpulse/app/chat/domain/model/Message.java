package com.socialpulse.app.chat.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private Instant timestamp;
    private MessageStatus status;

    public void markDelivered() {
        if (this.status == MessageStatus.SENT) {
            this.status = MessageStatus.DELIVERED;
        }
    }

    public void markRead() {
        if (this.status == MessageStatus.SENT || this.status == MessageStatus.DELIVERED) {
            this.status = MessageStatus.READ;
        }
    }

    public boolean canTransitionTo(MessageStatus newStatus) {
        return switch (newStatus) {
            case SENT -> false;
            case DELIVERED -> this.status == MessageStatus.SENT;
            case READ -> this.status == MessageStatus.DELIVERED || this.status == MessageStatus.SENT;
        };
    }
}
