package com.socialpulse.app.notification.adapter.web;

import com.socialpulse.app.security.permission.RequiresPermission;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;

import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.notification.application.dto.response.NotificationResponse;
import com.socialpulse.app.notification.application.dto.response.NotificationUnreadCountResponse;
import com.socialpulse.app.notification.application.usecase.GetNotificationsUseCase;
import com.socialpulse.app.notification.application.usecase.GetUnreadNotificationCountUseCase;
import com.socialpulse.app.notification.application.usecase.MarkAllNotificationsReadUseCase;
import com.socialpulse.app.notification.application.usecase.MarkNotificationReadUseCase;
import com.socialpulse.app.security.user.CustomUserDetails;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Database-backed notification APIs")
@Validated
public class NotificationController {
    private final GetNotificationsUseCase getNotificationsUseCase;
    private final GetUnreadNotificationCountUseCase getUnreadNotificationCountUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;

    public NotificationController(
            GetNotificationsUseCase getNotificationsUseCase,
            GetUnreadNotificationCountUseCase getUnreadNotificationCountUseCase,
            MarkNotificationReadUseCase markNotificationReadUseCase,
            MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase) {
        this.getNotificationsUseCase = getNotificationsUseCase;
        this.getUnreadNotificationCountUseCase = getUnreadNotificationCountUseCase;
        this.markNotificationReadUseCase = markNotificationReadUseCase;
        this.markAllNotificationsReadUseCase = markAllNotificationsReadUseCase;
    }

    @GetMapping
    @RequiresPermission.NotificationRead
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<NotificationResponse>>builder()
                .data(getNotificationsUseCase.getNotifications(page, size, currentUser))
                .build());
    }

    @GetMapping("/unread-count")
    @RequiresPermission.NotificationRead
    public ResponseEntity<ApiResponse<NotificationUnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.<NotificationUnreadCountResponse>builder()
                .data(getUnreadNotificationCountUseCase.getUnreadCount(currentUser))
                .build());
    }

    @PatchMapping("/{notificationId}/read")
    @RequiresPermission.NotificationUpdate
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.<NotificationResponse>builder()
                .data(markNotificationReadUseCase.markRead(notificationId, currentUser))
                .build());
    }

    @PatchMapping("/read-all")
    @RequiresPermission.NotificationUpdate
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        markAllNotificationsReadUseCase.markAllRead(currentUser);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("All notifications marked as read")
                .build());
    }
}
