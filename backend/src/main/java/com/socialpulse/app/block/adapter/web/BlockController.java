package com.socialpulse.app.block.adapter.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.block.application.usecase.BlockUserUseCase;
import com.socialpulse.app.block.application.usecase.GetBlockedUserIdsUseCase;
import com.socialpulse.app.block.application.usecase.IsBlockedUseCase;
import com.socialpulse.app.block.application.usecase.UnblockUserUseCase;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/blocks")
@Tag(name = "Blocks", description = "User block management APIs")
public class BlockController {

    private final BlockUserUseCase blockUserUseCase;
    private final UnblockUserUseCase unblockUserUseCase;
    private final GetBlockedUserIdsUseCase getBlockedUserIdsUseCase;
    private final IsBlockedUseCase isBlockedUseCase;

    public BlockController(BlockUserUseCase blockUserUseCase,
                           UnblockUserUseCase unblockUserUseCase,
                           GetBlockedUserIdsUseCase getBlockedUserIdsUseCase,
                           IsBlockedUseCase isBlockedUseCase) {
        this.blockUserUseCase = blockUserUseCase;
        this.unblockUserUseCase = unblockUserUseCase;
        this.getBlockedUserIdsUseCase = getBlockedUserIdsUseCase;
        this.isBlockedUseCase = isBlockedUseCase;
    }

    @PostMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Block a user")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        blockUserUseCase.blockUser(currentUser.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<Void>builder()
                        .message("Successfully blocked user")
                        .build());
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Unblock a user")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        unblockUserUseCase.unblockUser(currentUser.getId(), userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Successfully unblocked user")
                .build());
    }

    @GetMapping("/ids")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all blocked user IDs")
    public ResponseEntity<ApiResponse<List<Long>>> getBlockedUserIds(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        List<Long> blockedIds = getBlockedUserIdsUseCase.getBlockedUserIds(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<List<Long>>builder()
                .data(blockedIds)
                .message("Successfully retrieved blocked user IDs")
                .build());
    }

    @GetMapping("/{userId}/is-blocked")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check if a user is blocked by me")
    public ResponseEntity<ApiResponse<Boolean>> isUserBlocked(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        boolean blocked = isBlockedUseCase.isBlocked(currentUser.getId(), userId);
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .data(blocked)
                .message("Checked block status successfully")
                .build());
    }
}
