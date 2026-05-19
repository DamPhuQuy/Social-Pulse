package com.socialpulse.app.post.adapter.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.post.application.dto.request.PostCreationRequest;
import com.socialpulse.app.post.application.dto.request.PostReactionRequest;
import com.socialpulse.app.post.application.dto.request.PostUpdateRequest;
import com.socialpulse.app.post.application.dto.response.PostCreationResponse;
import com.socialpulse.app.post.application.dto.response.PostReactionResponse;
import com.socialpulse.app.post.application.dto.response.PostTopicResponse;
import com.socialpulse.app.post.application.dto.response.PostUpdateResponse;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;
import com.socialpulse.app.post.application.dto.response.ViewPostResponse;
import com.socialpulse.app.post.application.service.PostTopicCatalog;
import com.socialpulse.app.post.application.usecase.CreatePostUseCase;
import com.socialpulse.app.post.application.usecase.DeletePostUseCase;
import com.socialpulse.app.post.application.usecase.EditPostUseCase;
import com.socialpulse.app.post.application.usecase.GetUserPostsUseCase;
import com.socialpulse.app.post.application.usecase.ReactPostUseCase;
import com.socialpulse.app.post.application.usecase.ViewPostUseCase;
import com.socialpulse.app.security.user.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/posts")
@Tag(name = "Posts", description = "Post management APIs")
public class PostController {
    private final CreatePostUseCase createPostUseCase;
    private final ViewPostUseCase viewPostUseCase;
    private final ReactPostUseCase reactPostUseCase;
    private final DeletePostUseCase deletePostUseCase;
    private final EditPostUseCase editPostUseCase;
    private final GetUserPostsUseCase getUserPostsUseCase;

    public PostController(CreatePostUseCase createPostUseCase,
                          ViewPostUseCase viewPostUseCase,
                          ReactPostUseCase reactPostUseCase,
                          DeletePostUseCase deletePostUseCase,
                          EditPostUseCase editPostUseCase,
                          GetUserPostsUseCase getUserPostsUseCase) {
        this.createPostUseCase = createPostUseCase;
        this.viewPostUseCase = viewPostUseCase;
        this.reactPostUseCase = reactPostUseCase;
        this.deletePostUseCase = deletePostUseCase;
        this.editPostUseCase = editPostUseCase;
        this.getUserPostsUseCase = getUserPostsUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('post:create')")
    @Operation(
            summary = "Create post",
            description = "Create a new post for current authenticated user",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Post created successfully"
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
                )
            }
    )
    public ResponseEntity<ApiResponse<PostCreationResponse>> createPost(
            @RequestBody @Valid PostCreationRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        PostCreationResponse response = createPostUseCase.createPost(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.<PostCreationResponse>builder().data(response).build());
    }

    @GetMapping("/{postId}")
    @PreAuthorize("hasAuthority('post:read')")
    public ResponseEntity<ApiResponse<ViewPostResponse>> viewPost(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.<ViewPostResponse>builder().data(viewPostUseCase.viewPost(postId, currentUser)).build());
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('post:read')")
    @Operation(summary = "Get posts by user", description = "Get paginated posts for a user timeline/profile")
    public ResponseEntity<ApiResponse<PageResponse<UserPostResponse>>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<UserPostResponse>>builder()
                .data(getUserPostsUseCase.getUserPosts(userId, page, size, currentUser))
                .build());
    }

    @GetMapping("/topics")
    @PreAuthorize("hasAuthority('post:read')")
    @Operation(summary = "List post topics", description = "List selectable post topics used by create/edit post flows")
    public ResponseEntity<ApiResponse<List<PostTopicResponse>>> getPostTopics() {
        return ResponseEntity.ok(ApiResponse.<List<PostTopicResponse>>builder()
                .data(PostTopicCatalog.all())
                .build());
    }

    @PostMapping("/react")
    @PreAuthorize("hasAuthority('post:react')")
    public ResponseEntity<ApiResponse<PostReactionResponse>> react(
            @RequestBody @Valid PostReactionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.<PostReactionResponse>builder().data(reactPostUseCase.react(request, currentUser)).build());
    }

    @DeleteMapping("/{postId}")
    @PreAuthorize("hasAnyAuthority('post:delete', 'post:manage')")
    @Operation(summary = "Delete post", description = "Soft delete a post. Only the author or an admin can delete.")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails currentUser) {
        deletePostUseCase.deletePost(postId, currentUser);
        return ResponseEntity.ok(ApiResponse.<Void>builder().message("Post deleted successfully").build());
    }

    @PutMapping("/{postId}")
    @PreAuthorize("hasAnyAuthority('post:update', 'post:manage')")
    @Operation(summary = "Edit post", description = "Edit an existing post")
    public ResponseEntity<ApiResponse<PostUpdateResponse>> editPost(
            @PathVariable Long postId,
            @RequestBody @Valid PostUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        PostUpdateResponse response = editPostUseCase.editPost(postId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.<PostUpdateResponse>builder().data(response).message("Post updated successfully").build());
    }

}
