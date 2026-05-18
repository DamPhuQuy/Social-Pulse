package com.socialpulse.app.comment.adapter.web;

import com.socialpulse.app.security.permission.RequiresPermission;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.socialpulse.app.comment.application.dto.request.CommentCreationRequest;
import com.socialpulse.app.comment.application.dto.request.CommentReactionRequest;
import com.socialpulse.app.comment.application.dto.request.CommentUpdateRequest;
import com.socialpulse.app.comment.application.dto.response.CommentCreationResponse;
import com.socialpulse.app.comment.application.dto.response.CommentReactionResponse;
import com.socialpulse.app.comment.application.usecase.CreateCommentUseCase;
import com.socialpulse.app.comment.application.usecase.DeleteCommentUseCase;
import com.socialpulse.app.comment.application.usecase.GetCommentRepliesUseCase;
import com.socialpulse.app.comment.application.usecase.UpdateCommentUseCase;
import com.socialpulse.app.comment.application.usecase.ReactCommentUseCase;
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
    private final UpdateCommentUseCase updateCommentUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;
    private final GetTopLevelCommentsUseCase getTopLevelCommentsUseCase;
    private final GetCommentRepliesUseCase getCommentRepliesUseCase;
    private final ReactCommentUseCase reactCommentUseCase;

    public CommentController(CreateCommentUseCase createCommentUseCase,
                           UpdateCommentUseCase updateCommentUseCase,
                           DeleteCommentUseCase deleteCommentUseCase,
                           GetTopLevelCommentsUseCase getTopLevelCommentsUseCase,
                           GetCommentRepliesUseCase getCommentRepliesUseCase,
                           ReactCommentUseCase reactCommentUseCase) {
        this.createCommentUseCase = createCommentUseCase;
        this.updateCommentUseCase = updateCommentUseCase;
        this.deleteCommentUseCase = deleteCommentUseCase;
        this.getTopLevelCommentsUseCase = getTopLevelCommentsUseCase;
        this.getCommentRepliesUseCase = getCommentRepliesUseCase;
        this.reactCommentUseCase = reactCommentUseCase;
    }

    @PostMapping
    @RequiresPermission.CommentCreate
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
    @RequiresPermission.CommentRead
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

    @GetMapping("/{commentId}/replies")
    @RequiresPermission.CommentRead
    @Operation(summary = "Get comment replies", description = "Get direct replies for a comment with offset and limit")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getCommentReplies(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") Long lastId,
            @RequestParam(defaultValue = "10") int limit) {

        List<CommentResponse> responses = getCommentRepliesUseCase.getReplies(postId, commentId, lastId, limit);

        return ResponseEntity.ok(ApiResponse.<List<CommentResponse>>builder()
                .code(200)
                .message("Replies fetched successfully.")
                .data(responses)
                .build());
    }

    @PostMapping("/{commentId}/react")
    @RequiresPermission.CommentReact
    @Operation(summary = "React to comment", description = "Create, update, or remove a reaction on a comment")
    public ResponseEntity<ApiResponse<CommentReactionResponse>> reactComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentReactionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        CommentReactionResponse response = reactCommentUseCase.react(postId, commentId, request, currentUser);

        return ResponseEntity.ok(ApiResponse.<CommentReactionResponse>builder()
                .code(200)
                .message("Comment reaction updated successfully.")
                .data(response)
                .build());
    }

    @PutMapping("/{commentId}")
    @RequiresPermission.CommentUpdateOrManage
    @Operation(
        summary = "Update comment",
        description = "Update a comment content",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Comment updated successfully"
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
                description = "Forbidden - Not the owner of the comment"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Comment not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        }
    )
    public ResponseEntity<ApiResponse<CommentCreationResponse>> updateComment(
        @PathVariable Long postId,
        @PathVariable Long commentId,
        @AuthenticationPrincipal CustomUserDetails currentUser,
        @RequestBody @Valid CommentUpdateRequest request) {

        CommentCreationResponse response = updateCommentUseCase.updateComment(postId, commentId, request, currentUser);

        return ResponseEntity.ok(ApiResponse.<CommentCreationResponse>builder()
                .code(200)
                .message("Comment updated successfully.")
                .data(response)
                .build());
    }

    @DeleteMapping("/{commentId}")
    @RequiresPermission.CommentDeleteOrManage
    @Operation(
        summary = "Delete comment",
        description = "Soft delete a comment (only owner can delete)",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Comment deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Forbidden - Not the owner of the comment"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Comment not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        }
    )
    public ResponseEntity<ApiResponse<Void>> deleteComment(
        @PathVariable Long postId,
        @PathVariable Long commentId,
        @AuthenticationPrincipal CustomUserDetails currentUser) {

        deleteCommentUseCase.deleteComment(postId, commentId, currentUser);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Comment deleted successfully.")
                .build());
    }

}
