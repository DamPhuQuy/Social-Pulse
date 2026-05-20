package com.socialpulse.app.bookmark.adapter.web;

import com.socialpulse.app.security.permission.RequiresPermission;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.socialpulse.app.bookmark.application.dto.response.BookmarkResponse;
import com.socialpulse.app.bookmark.application.usecase.CreateBookmarkUseCase;
import com.socialpulse.app.bookmark.application.usecase.DeleteBookmarkUseCase;
import com.socialpulse.app.bookmark.application.usecase.GetBookmarksUseCase;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;

@RestController
@RequestMapping("/api/v1/bookmarks")
@Tag(name = "Bookmarks", description = "Saved posts APIs")
@Validated
public class BookmarkController {
    private final CreateBookmarkUseCase createBookmarkUseCase;
    private final DeleteBookmarkUseCase deleteBookmarkUseCase;
    private final GetBookmarksUseCase getBookmarksUseCase;

    public BookmarkController(
            CreateBookmarkUseCase createBookmarkUseCase,
            DeleteBookmarkUseCase deleteBookmarkUseCase,
            GetBookmarksUseCase getBookmarksUseCase) {
        this.createBookmarkUseCase = createBookmarkUseCase;
        this.deleteBookmarkUseCase = deleteBookmarkUseCase;
        this.getBookmarksUseCase = getBookmarksUseCase;
    }

    @PostMapping("/{postId}")
    @RequiresPermission.BookmarkCreate
    public ResponseEntity<ApiResponse<BookmarkResponse>> createBookmark(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.<BookmarkResponse>builder()
                .data(createBookmarkUseCase.createBookmark(postId, currentUser))
                .build());
    }

    @DeleteMapping("/{postId}")
    @RequiresPermission.BookmarkDelete
    public ResponseEntity<ApiResponse<Void>> deleteBookmark(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        deleteBookmarkUseCase.deleteBookmark(postId, currentUser);
        return ResponseEntity.ok(ApiResponse.<Void>builder().message("Bookmark removed successfully").build());
    }

    @GetMapping
    @RequiresPermission.BookmarkRead
    public ResponseEntity<ApiResponse<PageResponse<UserPostResponse>>> getBookmarks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<UserPostResponse>>builder()
                .data(getBookmarksUseCase.getBookmarks(page, size, currentUser))
                .build());
    }
}
