package com.socialpulse.app.bookmark.adapter.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.bookmark.application.service.BookmarkService;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.feed.application.dto.response.FeedItemResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Bookmarks", description = "Developer Reading List & Bookmarking APIs")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping("/posts/{postId}/bookmark")
    @Operation(summary = "Toggle bookmark", description = "Save or remove a post from developer reading list")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleBookmark(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        boolean bookmarked = bookmarkService.toggleBookmark(postId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .data(Map.of("postId", postId, "bookmarked", bookmarked))
                .build());
    }

    @GetMapping("/bookmarks")
    @Operation(summary = "Get bookmarks", description = "Get saved developer posts for authenticated user")
    public ResponseEntity<ApiResponse<List<FeedItemResponse>>> getMyBookmarks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        List<FeedItemResponse> items = bookmarkService.getMyBookmarks(currentUser.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.<List<FeedItemResponse>>builder().data(items).build());
    }
}
