package com.socialpulse.app.comment.adapter.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.socialpulse.app.comment.application.dto.request.CommentCreationRequest;
import com.socialpulse.app.comment.application.dto.response.CommentCreationResponse;
import com.socialpulse.app.comment.application.usecase.CreateCommentUseCase;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.socialpulse.app.comment.application.usecase.GetTopLevelCommentsUseCase;
import com.socialpulse.app.comment.application.dto.response.CommentResponse;
import java.util.List;

@Controller
@RequestMapping("/api/v1/posts/{postId}/comments")
@Tag(name = "Comments", description = "Comment management APIs")
public class CommentController {
    private final CreateCommentUseCase createCommentUseCase;
    private final GetTopLevelCommentsUseCase getTopLevelCommentsUseCase;

    public CommentController(CreateCommentUseCase createCommentUseCase, GetTopLevelCommentsUseCase getTopLevelCommentsUseCase) {
        this.createCommentUseCase = createCommentUseCase;
        this.getTopLevelCommentsUseCase = getTopLevelCommentsUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('comment:create')")
    @Operation(
        summary = "Create comment",
        description = "Create a comment on a post",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Comment created successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request data"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Forbidden"
             ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        }
    )
    public ResponseEntity<ApiResponse<CommentCreationResponse>> createComment(
        @PathVariable Long postId,
        @AuthenticationPrincipal CustomUserDetails currentUser,
        @RequestBody @Valid CommentCreationRequest request) {

        CommentCreationResponse response = createCommentUseCase.createComment(postId, request, currentUser);

        return ResponseEntity.ok(ApiResponse.<CommentCreationResponse>builder()
                .code(200)
                .message("Comment created successfully.")
                .data(response)
                .build());
    }

    @GetMapping
    @Operation(summary = "Get top level comments", description = "Get top level comments for a post with offset and limit")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getTopLevelComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") Long lastId,
            @RequestParam(defaultValue = "10") int limit) {

        List<CommentResponse> responses = getTopLevelCommentsUseCase.getTopLevelComments(postId, lastId, limit);

        return ResponseEntity.ok(ApiResponse.<List<CommentResponse>>builder()
                .code(200)
                .message("Comments fetched successfully.")
                .data(responses)
                .build());
    }
    
    
}

