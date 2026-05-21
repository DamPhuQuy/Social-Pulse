package com.socialpulse.app.chat.adapter.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.socialpulse.app.chat.application.dto.request.CreateConversationRequest;
import com.socialpulse.app.chat.application.dto.response.ConversationListResponse;
import com.socialpulse.app.chat.application.dto.response.ConversationResponse;
import com.socialpulse.app.chat.application.dto.response.MessageHistoryResponse;
import com.socialpulse.app.chat.application.usecase.CreateConversationUseCase;
import com.socialpulse.app.chat.application.usecase.GetConversationsUseCase;
import com.socialpulse.app.chat.application.usecase.GetMessageHistoryUseCase;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.security.permission.RequiresPermission;
import com.socialpulse.app.security.user.CustomUserDetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final CreateConversationUseCase createConversationUseCase;
    private final GetConversationsUseCase getConversationsUseCase;
    private final GetMessageHistoryUseCase getMessageHistoryUseCase;

    @PostMapping("/conversations")
    @RequiresPermission.ChatCreate
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @RequestBody @Valid CreateConversationRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        ConversationResponse response = createConversationUseCase.createConversation(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ConversationResponse>builder()
                        .data(response)
                        .message("Conversation created successfully")
                        .build());
    }

    @GetMapping("/conversations")
    @RequiresPermission.ChatRead
    public ResponseEntity<ApiResponse<List<ConversationListResponse>>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        List<ConversationListResponse> conversations = getConversationsUseCase.getConversations(page, size, currentUser);
        return ResponseEntity.ok(ApiResponse.<List<ConversationListResponse>>builder()
                .data(conversations)
                .build());
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @RequiresPermission.ChatRead
    public ResponseEntity<ApiResponse<MessageHistoryResponse>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Max(100) int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        MessageHistoryResponse history = getMessageHistoryUseCase.getHistory(conversationId, cursor, size, currentUser);
        return ResponseEntity.ok(ApiResponse.<MessageHistoryResponse>builder()
                .data(history)
                .build());
    }
}
