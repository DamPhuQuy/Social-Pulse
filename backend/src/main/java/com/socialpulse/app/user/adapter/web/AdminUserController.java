package com.socialpulse.app.user.adapter.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.user.application.dto.request.AdminAssignRoleRequest;
import com.socialpulse.app.user.application.dto.response.AdminUserResponse;
import com.socialpulse.app.user.application.usecase.AdminAssignUserRoleUseCase;
import com.socialpulse.app.user.application.usecase.AdminGetUserDetailsUseCase;
import com.socialpulse.app.user.application.usecase.AdminLockUnlockUserUseCase;
import com.socialpulse.app.user.application.usecase.AdminSearchUsersUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin Users Management", description = "Admin APIs for managing user accounts")
@PreAuthorize("hasAuthority('user:manage')")
public class AdminUserController {

    private final AdminSearchUsersUseCase adminSearchUsersUseCase;
    private final AdminGetUserDetailsUseCase adminGetUserDetailsUseCase;
    private final AdminLockUnlockUserUseCase adminLockUnlockUserUseCase;
    private final AdminAssignUserRoleUseCase adminAssignUserRoleUseCase;

    public AdminUserController(AdminSearchUsersUseCase adminSearchUsersUseCase,
                               AdminGetUserDetailsUseCase adminGetUserDetailsUseCase,
                               AdminLockUnlockUserUseCase adminLockUnlockUserUseCase,
                               AdminAssignUserRoleUseCase adminAssignUserRoleUseCase) {
        this.adminSearchUsersUseCase = adminSearchUsersUseCase;
        this.adminGetUserDetailsUseCase = adminGetUserDetailsUseCase;
        this.adminLockUnlockUserUseCase = adminLockUnlockUserUseCase;
        this.adminAssignUserRoleUseCase = adminAssignUserRoleUseCase;
    }

    @GetMapping
    @Operation(summary = "Search/Filter Users", description = "Admin searches users by username or display name with pagination")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<AdminUserResponse>>builder()
                .data(adminSearchUsersUseCase.searchUsers(query, page, size))
                .build());
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get User Details", description = "Admin gets full details of a user account")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUserDetails(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.<AdminUserResponse>builder()
                .data(adminGetUserDetailsUseCase.getUserDetails(userId))
                .build());
    }

    @PutMapping("/{userId}/lock")
    @Operation(summary = "Lock User Account", description = "Admin locks a user account")
    public ResponseEntity<ApiResponse<AdminUserResponse>> lockUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.<AdminUserResponse>builder()
                .data(adminLockUnlockUserUseCase.lockUser(userId))
                .message("User account has been locked successfully")
                .build());
    }

    @PutMapping("/{userId}/unlock")
    @Operation(summary = "Unlock User Account", description = "Admin unlocks a user account")
    public ResponseEntity<ApiResponse<AdminUserResponse>> unlockUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.<AdminUserResponse>builder()
                .data(adminLockUnlockUserUseCase.unlockUser(userId))
                .message("User account has been unlocked successfully")
                .build());
    }

    @PutMapping("/{userId}/roles")
    @Operation(summary = "Assign Roles to User", description = "Admin assigns new roles to a user")
    public ResponseEntity<ApiResponse<AdminUserResponse>> assignRoles(
            @PathVariable Long userId,
            @RequestBody @Valid AdminAssignRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.<AdminUserResponse>builder()
                .data(adminAssignUserRoleUseCase.assignRoles(userId, request))
                .message("User roles updated successfully")
                .build());
    }
}
