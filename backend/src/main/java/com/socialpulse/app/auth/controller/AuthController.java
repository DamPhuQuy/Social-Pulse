package com.socialpulse.app.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.auth.dto.request.EmailVerificationRequest;
import com.socialpulse.app.auth.service.AuthService;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.user.dto.request.UserCreationRequest;
import com.socialpulse.app.user.dto.response.UserCreationResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserCreationResponse>> registerUser(@Valid @RequestBody UserCreationRequest request) {
        UserCreationResponse result = authService.register(request);

        ApiResponse<UserCreationResponse> response = ApiResponse.<UserCreationResponse>builder()
                .code(201)
                .message("User registered successfully. OTP has been sent to your email")
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
                .message("Email verified successfully")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }
}
