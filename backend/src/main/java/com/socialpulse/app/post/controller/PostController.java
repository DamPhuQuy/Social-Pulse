package com.socialpulse.app.post.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.post.dto.request.PostCreationRequest;
import com.socialpulse.app.post.dto.request.PostReactionRequest;
import com.socialpulse.app.post.dto.response.PostCreationResponse;
import com.socialpulse.app.post.dto.response.PostReactionResponse;
import com.socialpulse.app.post.service.PostService;
import com.socialpulse.app.post.dto.response.ViewPostResponse;
import com.socialpulse.app.post.dto.request.ViewPostRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/posts")
@Tag(name = "Posts", description = "Post management APIs")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_POST')")
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
    public ResponseEntity<PostCreationResponse> createPost(
            @RequestBody @Valid PostCreationRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.createPost(request, currentUser.getId()));
    }
    
    @GetMapping("/{postId}")
    @PreAuthorize("hasAuthority('READ_POSTS')")
    public ResponseEntity<ViewPostResponse> viewPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        ViewPostRequest request = ViewPostRequest.builder()
                .postId(postId)
                .build();
        return ResponseEntity.status(HttpStatus.OK)
                .body(postService.viewPost(request));

    @PostMapping("/upvote")
    public ResponseEntity<PostReactionResponse> upvote(
        @RequestBody @Valid PostReactionRequest request,
        @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(postService.upvote(request, currentUser.getId()));
    }

    @PostMapping("/downvote")
    public ResponseEntity<PostReactionResponse> downvote(
        @RequestBody @Valid PostReactionRequest request,
        @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(postService.downvote(request, currentUser.getId()));
    }
}
