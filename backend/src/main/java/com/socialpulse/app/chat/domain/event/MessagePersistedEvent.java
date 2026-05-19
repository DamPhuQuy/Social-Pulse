package com.socialpulse.app.chat.domain.event;

import com.socialpulse.app.chat.domain.model.Message;

/**
 * Domain event published after a message is successfully persisted.
 * Used to decouple message persistence from delivery logic.
 */
public record MessagePersistedEvent(Message message, Long recipientId) {}
