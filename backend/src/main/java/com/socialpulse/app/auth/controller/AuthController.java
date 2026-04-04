package com.socialpulse.app.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.socialpulse.app.auth.dto.request.EmailVerificationRequest;
import com.socialpulse.app.auth.dto.request.LoginRequest;
import com.socialpulse.app.auth.dto.response.LoginResponse;
import com.socialpulse.app.auth.service.AuthService;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.user.dto.request.UserCreationRequest;
import com.socialpulse.app.user.dto.response.UserCreationResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Registers a new user and sends an OTP code to their email for verification")
    public ResponseEntity<ApiResponse<UserCreationResponse>> registerUser(@Valid @RequestBody UserCreationRequest request) {
        UserCreationResponse result = authService.register(request);

        ApiResponse<UserCreationResponse> response = ApiResponse.<UserCreationResponse>builder()
                .code(201)
                .message("User registered successfully. Please check your email for the OTP code to verify your account.")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with OTP", description = "Verifies the user's email using the provided OTP code")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        authService.verifyEmail(request);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(200)
                .message("Email verified successfully.")
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * POST /api/v1/auth/login
     *
     * Nhận email + password → xác thực → trả JWT access token.
     * Client lưu token và gửi kèm mọi request protected:
     *   Header: "Authorization: Bearer <accessToken>"
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with email/password and receive JWT access token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse result = authService.login(request);

        ApiResponse<LoginResponse> response = ApiResponse.<LoginResponse>builder()
                .code(200)
                .message("Login successful.")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }
}
