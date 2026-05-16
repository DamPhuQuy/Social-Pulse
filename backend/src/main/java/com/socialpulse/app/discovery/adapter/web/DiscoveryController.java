package com.socialpulse.app.discovery.adapter.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.discovery.application.dto.response.SearchUserResponse;
import com.socialpulse.app.discovery.application.dto.response.TrendingHashtagResponse;
import com.socialpulse.app.discovery.application.usecase.GetPostsByHashtagUseCase;
import com.socialpulse.app.discovery.application.usecase.GetPostsByMentionUseCase;
import com.socialpulse.app.discovery.application.usecase.GetTrendingHashtagsUseCase;
import com.socialpulse.app.discovery.application.usecase.SearchPostsUseCase;
import com.socialpulse.app.discovery.application.usecase.SearchUsersUseCase;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/discovery")
@Tag(name = "Discovery", description = "Search and discovery APIs")
public class DiscoveryController {
    private final SearchUsersUseCase searchUsersUseCase;
    private final SearchPostsUseCase searchPostsUseCase;
    private final GetPostsByHashtagUseCase getPostsByHashtagUseCase;
    private final GetPostsByMentionUseCase getPostsByMentionUseCase;
    private final GetTrendingHashtagsUseCase getTrendingHashtagsUseCase;

    public DiscoveryController(
            SearchUsersUseCase searchUsersUseCase,
            SearchPostsUseCase searchPostsUseCase,
            GetPostsByHashtagUseCase getPostsByHashtagUseCase,
            GetPostsByMentionUseCase getPostsByMentionUseCase,
            GetTrendingHashtagsUseCase getTrendingHashtagsUseCase) {
        this.searchUsersUseCase = searchUsersUseCase;
        this.searchPostsUseCase = searchPostsUseCase;
        this.getPostsByHashtagUseCase = getPostsByHashtagUseCase;
        this.getPostsByMentionUseCase = getPostsByMentionUseCase;
        this.getTrendingHashtagsUseCase = getTrendingHashtagsUseCase;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('discovery:read')")
    public ResponseEntity<ApiResponse<PageResponse<SearchUserResponse>>> searchUsers(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<SearchUserResponse>>builder()
                .data(searchUsersUseCase.searchUsers(query, page, size))
                .build());
    }

    @GetMapping("/posts")
    @PreAuthorize("hasAuthority('discovery:read')")
    public ResponseEntity<ApiResponse<PageResponse<UserPostResponse>>> searchPosts(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<UserPostResponse>>builder()
                .data(searchPostsUseCase.searchPosts(query, page, size))
                .build());
    }

    @GetMapping("/hashtags/trending")
    @PreAuthorize("hasAuthority('discovery:read')")
    public ResponseEntity<ApiResponse<List<TrendingHashtagResponse>>> getTrendingHashtags(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.<List<TrendingHashtagResponse>>builder()
                .data(getTrendingHashtagsUseCase.getTrendingHashtags(days, limit))
                .build());
    }

    @GetMapping("/hashtags/{hashtag}/posts")
    @PreAuthorize("hasAuthority('discovery:read')")
    public ResponseEntity<ApiResponse<PageResponse<UserPostResponse>>> getPostsByHashtag(
            @PathVariable String hashtag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<UserPostResponse>>builder()
                .data(getPostsByHashtagUseCase.getPostsByHashtag(hashtag, page, size))
                .build());
    }

    @GetMapping("/mentions/{username}/posts")
    @PreAuthorize("hasAuthority('discovery:read')")
    public ResponseEntity<ApiResponse<PageResponse<UserPostResponse>>> getPostsByMention(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<UserPostResponse>>builder()
                .data(getPostsByMentionUseCase.getPostsByMention(username, page, size))
                .build());
    }
}
