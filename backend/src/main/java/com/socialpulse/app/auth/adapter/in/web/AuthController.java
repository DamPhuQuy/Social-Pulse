package com.socialpulse.app.auth.adapter.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.auth.application.dto.TokenPair;
import com.socialpulse.app.auth.application.dto.mapper.AuthMapper;
import com.socialpulse.app.auth.application.dto.request.EmailVerificationRequest;
import com.socialpulse.app.auth.application.dto.request.ForgotPasswordRequest;
import com.socialpulse.app.auth.application.dto.request.LoginRequest;
import com.socialpulse.app.auth.application.dto.request.ResendOtpRequest;
import com.socialpulse.app.auth.application.dto.request.ResetPasswordRequest;
import com.socialpulse.app.auth.application.dto.request.VerifyOtpRequest;
import com.socialpulse.app.auth.application.dto.response.LoginResponse;
import com.socialpulse.app.auth.application.port.in.JwtUseCase;
import com.socialpulse.app.auth.application.port.in.LoginUseCase;
import com.socialpulse.app.auth.application.port.in.PasswordResetUseCase;
import com.socialpulse.app.auth.application.port.in.RefreshTokenUseCase;
import com.socialpulse.app.auth.application.port.in.RegisterUseCase;
import com.socialpulse.app.auth.application.port.in.VerifyEmailUseCase;
import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.user.application.dto.request.UserCreationRequest;
import com.socialpulse.app.user.application.dto.response.UserAuthorizedResponse;
import com.socialpulse.app.user.application.dto.response.UserCreationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and account recovery APIs")
public class AuthController {

    // Returns access token in JSON body, sets refresh token in HttpOnly cookie.
    private static final String REFRESH_TOKEN_COOKIE_NAME = "sp_refresh_token";

        private final RegisterUseCase registerUseCase;
        private final VerifyEmailUseCase verifyEmailUseCase;
        private final LoginUseCase loginUseCase;
        private final JwtUseCase jwtUseCase;
        private final RefreshTokenUseCase refreshTokenUseCase;
        private final PasswordResetUseCase passwordResetUseCase;
        private final AuthMapper authMapper;

        public AuthController(RegisterUseCase registerUseCase,
                                                  VerifyEmailUseCase verifyEmailUseCase,
                                                  LoginUseCase loginUseCase,
                                                  JwtUseCase jwtUseCase,
                                                  RefreshTokenUseCase refreshTokenUseCase,
                                                  PasswordResetUseCase passwordResetUseCase,
                                                  AuthMapper authMapper) {
                this.registerUseCase = registerUseCase;
                this.verifyEmailUseCase = verifyEmailUseCase;
                this.loginUseCase = loginUseCase;
        this.jwtUseCase = jwtUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.passwordResetUseCase = passwordResetUseCase;
                this.authMapper = authMapper;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register user",
            description = "Create a new account and send OTP to email",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or user already exists"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public ResponseEntity<ApiResponse<UserCreationResponse>> registerUser(@Valid @RequestBody UserCreationRequest request) {
                UserCreationResponse result = registerUseCase.register(request);

        ApiResponse<UserCreationResponse> response = ApiResponse.<UserCreationResponse>builder()
                .code(201)
                .message("User registered successfully. Please check your email for the OTP code to verify your account.")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-email")
    @Operation(
            summary = "Verify email",
            description = "Verify account using OTP code",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many verification attempts"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
                verifyEmailUseCase.verifyEmail(request);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(200)
                .message("Email verified successfully.")
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Authenticate by email/password and return access token",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account is not verified"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invalid username or password"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423", description = "Account is locked"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenPair tokens = loginUseCase.login(request);
                long accessExpiresInMs = jwtUseCase.getAccessExpirationMs();
        ResponseCookie refreshCookie = buildRefreshCookie(tokens.refreshToken());
        LoginResponse result = authMapper.toLoginResponse(tokens, accessExpiresInMs);

        ApiResponse<LoginResponse> response = ApiResponse.<LoginResponse>builder()
                .code(200)
                .message("Login successful.")
                .data(result)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Issue a new access token from refresh-token cookie",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Access token refreshed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing, invalid, or expired refresh token")
            }
    )
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request);

        TokenPair rotatedTokens = refreshTokenUseCase.rotateTokens(refreshToken);
        long accessExpiresInMs = jwtUseCase.getAccessExpirationMs();
        ResponseCookie refreshCookie = buildRefreshCookie(rotatedTokens.refreshToken());
        LoginResponse result = authMapper.toLoginResponse(rotatedTokens, accessExpiresInMs);

        ApiResponse<LoginResponse> response = ApiResponse.<LoginResponse>builder()
                .code(200)
                .message("Token refreshed.")
                .data(result)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout",
            description = "Clear refresh-token cookie",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        refreshTokenUseCase.revokeCurrentToken(refreshToken);

        // Remove refresh token cookie by setting maxAge=0.
        ResponseCookie clearRefreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false) // TODO: set true when deploying with HTTPS
                .path("/api/v1/auth")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(200)
                .message("Logout successful.")
                .data(null)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString())
                .body(response);
    }

        private ResponseCookie buildRefreshCookie(String refreshToken) {
                long refreshExpiresInMs = jwtUseCase.getRefreshExpirationMs();

                return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                                .httpOnly(true)
                                .secure(false) // TODO: set true when deploying with HTTPS
                                .path("/api/v1/auth")
                                .sameSite("Lax")
                                .maxAge(refreshExpiresInMs / 1000)
                                .build();
        }

    @GetMapping("/me")
    @Operation(
            summary = "Get current user",
            description = "Return profile of authenticated user",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User details retrieved successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    public ResponseEntity<ApiResponse<UserAuthorizedResponse>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails user) {
                UserAuthorizedResponse response = authMapper.toUserAuthorizedResponse(user.getUser());

        return ResponseEntity.ok(ApiResponse.<UserAuthorizedResponse>builder()
                .code(200)
                .message("User details retrieved successfully.")
                .data(response)
                .build());
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request password reset",
            description = "Send OTP for password reset",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Email not found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetUseCase.processForgotPassword(request);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(200)
                .message("Mã OTP đã được gửi đến email của bạn.")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-otp")
    @Operation(
            summary = "Resend OTP",
            description = "Resend OTP to email",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "New OTP sent successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many requests"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Email not found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
            }
    )
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        passwordResetUseCase.processResendOtp(request);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(200)
                .message("Mã OTP mới đã được gửi thành công.")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    @Operation(
            summary = "Verify reset-password OTP",
            description = "Verify OTP code sent by forgot-password flow",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP verified successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Email not found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many verification attempts")
            }
    )
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        passwordResetUseCase.processVerifyOtp(request);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(200)
                .message("OTP verified successfully.")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password",
            description = "Verify OTP and set new password",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Email not found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many verification attempts")
            }
    )
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetUseCase.processResetPassword(request);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(200)
                .message("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới.")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }
}
