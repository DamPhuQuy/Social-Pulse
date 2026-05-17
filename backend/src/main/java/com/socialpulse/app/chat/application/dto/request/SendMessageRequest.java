package com.socialpulse.app.chat.application.dto.request;

public record SendMessageRequest(Long conversationId, String content) {}
