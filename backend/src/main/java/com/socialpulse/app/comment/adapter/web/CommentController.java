package com.socialpulse.app.comment.adapter.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
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

@Controller
@RequestMapping("/api/v1/comments")
@Tag(name = "Comments", description = "Comment management APIs")
public class CommentController {
    private final CreateCommentUseCase createCommentUseCase;

    public CommentController(CreateCommentUseCase createCommentUseCase) {
        this.createCommentUseCase = createCommentUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_COMMENT')")
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
    public ResponseEntity<ApiResponse<CommentCreationResponse>> createComment(@AuthenticationPrincipal CustomUserDetails currentUser, @RequestBody @Valid CommentCreationRequest request) {

        CommentCreationResponse response = createCommentUseCase.createComment(request, currentUser);

        return ResponseEntity.ok(ApiResponse.<CommentCreationResponse>builder()
                .code(200)
                .message("Comment created successfully.")
                .data(response)
                .build());
    }
}

