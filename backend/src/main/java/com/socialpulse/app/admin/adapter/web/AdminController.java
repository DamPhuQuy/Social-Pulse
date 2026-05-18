package com.socialpulse.app.admin.adapter.web;

import com.socialpulse.app.security.permission.RequiresPermission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.admin.application.dto.AdminUserResponse;
import com.socialpulse.app.admin.application.dto.BanUserRequest;
import com.socialpulse.app.admin.application.dto.ChangeUserRoleRequest;
import com.socialpulse.app.admin.application.dto.SystemMetricsResponse;
import com.socialpulse.app.admin.application.usecase.GetSystemMetricsUseCase;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.user.application.service.UserRoleService;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@RequiresPermission.AdminAccess
@Tag(name = "Admin", description = "Admin management APIs")
public class AdminController {
    private final GetSystemMetricsUseCase getSystemMetricsUseCase;
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;

    public AdminController(GetSystemMetricsUseCase getSystemMetricsUseCase,
                           UserRepository userRepository,
                           UserRoleService userRoleService) {
        this.getSystemMetricsUseCase = getSystemMetricsUseCase;
        this.userRepository = userRepository;
        this.userRoleService = userRoleService;
    }

    // ── Metrics ──────────────────────────────────────────────────────────────

    @GetMapping("/metrics")
    @Operation(summary = "View system metrics", description = "period: LAST_7_DAYS | LAST_30_DAYS | LAST_90_DAYS | ALL_TIME")
    public ResponseEntity<ApiResponse<SystemMetricsResponse>> getMetrics(
            @RequestParam(defaultValue = "LAST_30_DAYS") String period) {
        return ResponseEntity.ok(ApiResponse.<SystemMetricsResponse>builder()
                .data(getSystemMetricsUseCase.getMetrics(period))
                .build());
    }

    @GetMapping("/metrics/export")
    @Operation(summary = "Export statistical report as CSV")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(defaultValue = "LAST_30_DAYS") String period) {
        byte[] csv = getSystemMetricsUseCase.exportCsv(period);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("metrics-" + period.toLowerCase() + ".csv").build());
        return ResponseEntity.ok().headers(headers).body(csv);
    }

    // ── User management ───────────────────────────────────────────────────────

    @GetMapping("/users")
    @RequiresPermission.UserManage
    @Operation(summary = "View account list", description = "List all users with pagination. Use 'query' param to search/filter by username or display name.")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> getUsers(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> result = (query != null && !query.isBlank())
                ? userRepository.searchByQuery(query, pageable)
                : userRepository.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<AdminUserResponse>>builder()
                .data(PageResponse.<AdminUserResponse>builder()
                        .items(result.getContent().stream().map(this::toAdminUserResponse).toList())
                        .page(result.getNumber())
                        .size(result.getSize())
                        .totalElements(result.getTotalElements())
                        .totalPages(result.getTotalPages())
                        .hasNext(result.hasNext())
                        .build())
                .build());
    }

    @GetMapping("/users/{userId}")
    @RequiresPermission.UserManage
    @Operation(summary = "View detail account")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUserDetail(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.<AdminUserResponse>builder()
                .data(toAdminUserResponse(user))
                .build());
    }

    @PatchMapping("/users/{userId}/ban")
    @RequiresPermission.UserModerate
    @Operation(summary = "Ban or unban a user", description = "ban=true to ban, ban=false to unban")
    public ResponseEntity<ApiResponse<Void>> banUser(
            @PathVariable Long userId,
            @RequestBody @Valid BanUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));
        if (Boolean.TRUE.equals(request.getBan())) {
            user.lockAccount();
        } else {
            user.activeAccount();
        }
        userRepository.save(user);
        String msg = Boolean.TRUE.equals(request.getBan()) ? "User banned" : "User unbanned";
        return ResponseEntity.ok(ApiResponse.<Void>builder().message(msg).build());
    }

    @PatchMapping("/users/{userId}/role")
    @RequiresPermission.UserManage
    @Operation(summary = "Change user roles", description = "Replace user's roles with the provided set")
    public ResponseEntity<ApiResponse<Void>> changeRole(
            @PathVariable Long userId,
            @RequestBody @Valid ChangeUserRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));
        userRoleService.assignRoles(user, request.getRoles());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.<Void>builder().message("Roles updated").build());
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        var roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(java.util.stream.Collectors.toSet());
        String displayName = user.getProfile() != null ? user.getProfile().getDisplayName() : null;
        String avatarUrl   = user.getProfile() != null ? user.getProfile().getAvatarUrl()   : null;
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .status(user.getStatus())
                .verification(user.getVerification())
                .isLocked(user.isLocked())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .roles(roles)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
