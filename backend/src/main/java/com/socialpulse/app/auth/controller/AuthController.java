package com.socialpulse.app.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.socialpulse.app.auth.dto.request.EmailVerificationRequest;
import com.socialpulse.app.auth.dto.request.LoginRequest;
import com.socialpulse.app.auth.dto.response.LoginResponse;
import com.socialpulse.app.auth.service.jwt.JwtService;
import com.socialpulse.app.auth.service.AuthService;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.user.dto.request.UserCreationRequest;
import com.socialpulse.app.user.dto.response.UserCreationResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "sp_access_token";

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
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
     * Nhận email + password → xác thực → set JWT vào HttpOnly cookie.
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with email/password and set JWT HttpOnly cookie")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        String accessToken = authService.login(request);
        long expiresInMs = jwtService.getExpirationMs();

        ResponseCookie authCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(expiresInMs / 1000)
                .build();

        LoginResponse result = LoginResponse.builder()
                .tokenType("Bearer")
                .expiresIn(expiresInMs)
                .build();

        ApiResponse<LoginResponse> response = ApiResponse.<LoginResponse>builder()
                .code(200)
                .message("Login successful.")
                .data(result)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Clear JWT HttpOnly cookie")
    public ResponseEntity<ApiResponse<Void>> logout() {
        ResponseCookie clearCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(200)
                .message("Logout successful.")
                .data(null)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(response);
    }

    @GetMapping("/session")
    @Operation(summary = "Session status", description = "Check current authentication status from HttpOnly cookie")
    public ResponseEntity<ApiResponse<Boolean>> getSession(Authentication authentication) {
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());

        if (!authenticated) {
            ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                    .code(401)
                    .message("Unauthenticated")
                    .data(false)
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                .code(200)
                .message("Authenticated")
                .data(true)
                .build();
        return ResponseEntity.ok(response);
    }
}
