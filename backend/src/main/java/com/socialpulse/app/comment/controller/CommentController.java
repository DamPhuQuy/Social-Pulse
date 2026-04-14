package com.socialpulse.app.comment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.comment.dto.request.CommentCreationRequest;
import com.socialpulse.app.comment.dto.response.CommentCreationResponse;
import com.socialpulse.app.comment.service.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/api/v1/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_COMMENT')")
    @Operation(summary = "Create a comment on a post or reply to another comment",
        description = "Create a comment on a post or reply to another comment. " +
                "If parentCommentId is provided, the comment will be a reply to the specified comment. " +
                "Otherwise, it will be a top-level comment on the post.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Comment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Post or parent comment not found"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to create comment")
        }
    )
    public ResponseEntity<CommentCreationResponse> createComment(@AuthenticationPrincipal CustomUserDetails currentUser, @RequestBody @Valid CommentCreationRequest request) {
        Long userId = currentUser.getId();

        return ResponseEntity.ok(commentService.createComment(request, userId));
    }
}
