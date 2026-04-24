package com.socialpulse.app.post.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.post.application.dto.request.PostCreationRequest;
import com.socialpulse.app.post.application.dto.request.PostReactionRequest;
import com.socialpulse.app.post.application.dto.response.PostCreationResponse;
import com.socialpulse.app.post.application.dto.response.PostReactionResponse;
import com.socialpulse.app.post.application.dto.response.ViewPostResponse;
import com.socialpulse.app.post.application.usecase.CreatePostUseCase;
import com.socialpulse.app.post.application.usecase.DeletePostUseCase;
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

    public PostController(CreatePostUseCase createPostUseCase,
                          ViewPostUseCase viewPostUseCase,
                          ReactPostUseCase reactPostUseCase,
                          DeletePostUseCase deletePostUseCase) {
        this.createPostUseCase = createPostUseCase;
        this.viewPostUseCase = viewPostUseCase;
        this.reactPostUseCase = reactPostUseCase;
        this.deletePostUseCase = deletePostUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') and hasAuthority('CREATE_POST')")
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

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') and hasAuthority('READ_POSTS')")
    public ResponseEntity<ApiResponse<ViewPostResponse>> viewPost(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.<ViewPostResponse>builder().data(viewPostUseCase.viewPost(id, currentUser)).build());
    }

    @PostMapping("/react")
    @PreAuthorize("hasRole('USER') and hasAuthority('REACT_POST')")
    public ResponseEntity<ApiResponse<PostReactionResponse>> react(
            @RequestBody @Valid PostReactionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.<PostReactionResponse>builder().data(reactPostUseCase.react(request, currentUser)).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Delete post", description = "Soft delete a post. Only the author or an admin can delete.")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        deletePostUseCase.deletePost(id, currentUser);
        return ResponseEntity.ok(ApiResponse.<Void>builder().message("Post deleted successfully").build());
    }

}

