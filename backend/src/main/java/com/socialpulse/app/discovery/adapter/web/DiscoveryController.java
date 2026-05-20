package com.socialpulse.app.discovery.adapter.web;

import java.util.List;

import com.socialpulse.app.security.permission.RequiresPermission;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.discovery.application.dto.request.SaveSearchHistoryRequest;
import com.socialpulse.app.discovery.application.dto.response.SearchHistoryResponse;
import com.socialpulse.app.discovery.application.dto.response.SearchUserResponse;
import com.socialpulse.app.discovery.application.dto.response.TrendingHashtagResponse;
import com.socialpulse.app.discovery.application.usecase.ClearSearchHistoryUseCase;
import com.socialpulse.app.discovery.application.usecase.DeleteSearchHistoryUseCase;
import com.socialpulse.app.discovery.application.usecase.GetPostsByHashtagUseCase;
import com.socialpulse.app.discovery.application.usecase.GetPostsByMentionUseCase;
import com.socialpulse.app.discovery.application.usecase.GetSearchHistoryUseCase;
import com.socialpulse.app.discovery.application.usecase.GetTrendingHashtagsUseCase;
import com.socialpulse.app.discovery.application.usecase.SaveSearchHistoryUseCase;
import com.socialpulse.app.discovery.application.usecase.SearchPostsUseCase;
import com.socialpulse.app.discovery.application.usecase.SearchUsersUseCase;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;

@RestController
@RequestMapping("/api/v1/discovery")
@Tag(name = "Discovery", description = "Search and discovery APIs")
@Validated
public class DiscoveryController {
    private final SearchUsersUseCase searchUsersUseCase;
    private final SearchPostsUseCase searchPostsUseCase;
    private final GetPostsByHashtagUseCase getPostsByHashtagUseCase;
    private final GetPostsByMentionUseCase getPostsByMentionUseCase;
    private final GetTrendingHashtagsUseCase getTrendingHashtagsUseCase;
    private final SaveSearchHistoryUseCase saveSearchHistoryUseCase;
    private final GetSearchHistoryUseCase getSearchHistoryUseCase;
    private final DeleteSearchHistoryUseCase deleteSearchHistoryUseCase;
    private final ClearSearchHistoryUseCase clearSearchHistoryUseCase;

    public DiscoveryController(
            SearchUsersUseCase searchUsersUseCase,
            SearchPostsUseCase searchPostsUseCase,
            GetPostsByHashtagUseCase getPostsByHashtagUseCase,
            GetPostsByMentionUseCase getPostsByMentionUseCase,
            GetTrendingHashtagsUseCase getTrendingHashtagsUseCase,
            SaveSearchHistoryUseCase saveSearchHistoryUseCase,
            GetSearchHistoryUseCase getSearchHistoryUseCase,
            DeleteSearchHistoryUseCase deleteSearchHistoryUseCase,
            ClearSearchHistoryUseCase clearSearchHistoryUseCase) {
        this.searchUsersUseCase = searchUsersUseCase;
        this.searchPostsUseCase = searchPostsUseCase;
        this.getPostsByHashtagUseCase = getPostsByHashtagUseCase;
        this.getPostsByMentionUseCase = getPostsByMentionUseCase;
        this.getTrendingHashtagsUseCase = getTrendingHashtagsUseCase;
        this.saveSearchHistoryUseCase = saveSearchHistoryUseCase;
        this.getSearchHistoryUseCase = getSearchHistoryUseCase;
        this.deleteSearchHistoryUseCase = deleteSearchHistoryUseCase;
        this.clearSearchHistoryUseCase = clearSearchHistoryUseCase;
    }

    @GetMapping("/users")
    @RequiresPermission.DiscoveryRead
    public ResponseEntity<ApiResponse<PageResponse<SearchUserResponse>>> searchUsers(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<SearchUserResponse>>builder()
                .data(searchUsersUseCase.searchUsers(query, page, size))
                .build());
    }

    @GetMapping("/posts")
    @RequiresPermission.DiscoveryRead
    public ResponseEntity<ApiResponse<PageResponse<UserPostResponse>>> searchPosts(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<UserPostResponse>>builder()
                .data(searchPostsUseCase.searchPosts(query, page, size))
                .build());
    }

    @GetMapping("/hashtags/trending")
    @RequiresPermission.DiscoveryRead
    public ResponseEntity<ApiResponse<List<TrendingHashtagResponse>>> getTrendingHashtags(
            @RequestParam(defaultValue = "7") @Max(365) int days,
            @RequestParam(defaultValue = "10") @Max(100) int limit) {
        return ResponseEntity.ok(ApiResponse.<List<TrendingHashtagResponse>>builder()
                .data(getTrendingHashtagsUseCase.getTrendingHashtags(days, limit))
                .build());
    }

    @GetMapping("/hashtags/{hashtag}/posts")
    @RequiresPermission.DiscoveryRead
    public ResponseEntity<ApiResponse<PageResponse<UserPostResponse>>> getPostsByHashtag(
            @PathVariable String hashtag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<UserPostResponse>>builder()
                .data(getPostsByHashtagUseCase.getPostsByHashtag(hashtag, page, size))
                .build());
    }

    @GetMapping("/mentions/{username}/posts")
    @RequiresPermission.DiscoveryRead
    public ResponseEntity<ApiResponse<PageResponse<UserPostResponse>>> getPostsByMention(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<UserPostResponse>>builder()
                .data(getPostsByMentionUseCase.getPostsByMention(username, page, size))
                .build());
    }

    @PostMapping("/history")
    @RequiresPermission.DiscoveryWrite
    @Operation(
            summary = "Save search history",
            description = "Save a search keyword to user's search history. Automatically handles duplicates and 20-item limit with FIFO.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search history saved successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    public ResponseEntity<ApiResponse<Void>> saveSearchHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Valid SaveSearchHistoryRequest request) {
        saveSearchHistoryUseCase.saveSearchHistory(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Search history saved successfully")
                .build());
    }

    @GetMapping("/history")
    @RequiresPermission.DiscoveryRead
    @Operation(
            summary = "Get search history",
            description = "Retrieve user's search history ordered by most recent first (updated_at DESC)",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search history retrieved successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    public ResponseEntity<ApiResponse<List<SearchHistoryResponse>>> getSearchHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.<List<SearchHistoryResponse>>builder()
                .data(getSearchHistoryUseCase.getSearchHistory(currentUser.getId()))
                .build());
    }

    @DeleteMapping("/history/{id}")
    @RequiresPermission.DiscoveryDelete
    @Operation(
            summary = "Delete a search history item",
            description = "Delete a specific search history item by ID",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search history deleted successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Search history not found")
            }
    )
    public ResponseEntity<ApiResponse<Void>> deleteSearchHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {
        deleteSearchHistoryUseCase.deleteSearchHistory(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Search history deleted successfully")
                .build());
    }

    @DeleteMapping("/history")
    @RequiresPermission.DiscoveryDelete
    @Operation(
            summary = "Clear all search history",
            description = "Delete all search history for the authenticated user",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search history cleared successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    public ResponseEntity<ApiResponse<Void>> clearSearchHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        clearSearchHistoryUseCase.clearSearchHistory(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Search history cleared successfully")
                .build());
    }
}
