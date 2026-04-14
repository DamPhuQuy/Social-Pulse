package com.socialpulse.app.auth.controller;

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

import com.socialpulse.app.auth.dto.TokenPair;
import com.socialpulse.app.auth.dto.request.EmailVerificationRequest;
import com.socialpulse.app.auth.dto.request.ForgotPasswordRequest;
import com.socialpulse.app.auth.dto.request.LoginRequest;
import com.socialpulse.app.auth.dto.request.ResendOtpRequest;
import com.socialpulse.app.auth.dto.request.ResetPasswordRequest;
import com.socialpulse.app.auth.dto.response.LoginResponse;
import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.auth.service.AuthService;
import com.socialpulse.app.auth.service.jwt.JwtService;
import com.socialpulse.app.auth.service.jwt.RefreshTokenService;
import com.socialpulse.app.auth.service.password.PasswordResetService;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.common.dto.response.AuthApiResponseSchemas;
import com.socialpulse.app.common.exception.ErrorResponse;
import com.socialpulse.app.user.dto.request.UserCreationRequest;
import com.socialpulse.app.user.dto.response.UserAuthorizedResponse;
import com.socialpulse.app.user.dto.response.UserCreationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    // returns access token with json body, sets refresh token in HttpOnly cookie
    private static final String REFRESH_TOKEN_COOKIE_NAME = "sp_refresh_token";

    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Registers a new user and sends an OTP code to their email for verification",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "201",
                            description = "User registered successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthApiResponseSchemas.UserCreation.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "code": 201,
                                              "message": "User registered successfully. Please check your email for the OTP code to verify your account.",
                                              "data": {
                                                "id": 1,
                                                "username": "phuquy123",
                                                "email": "phuquydam06@gmail.com",
                                                "message": "User created successfully"
                                              }
                                            }
                                            """)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Validation failed, passwords do not match, or user already exists",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "status": 400,
                                              "message": "User already exists",
                                              "timestamp": "2026-04-08T11:30:00"
                                            }
                                            """)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "500",
                            description = "Unexpected server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                                "status": 500,
                                                "message": "Unexpected server error",
                                                "timestamp": "2026-04-08T11:30:00"
                                            }
                                            """))
                    )
            }
    )
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
    @Operation(
            summary = "Verify email with OTP",
            description = "Verifies the user's email using the provided OTP code",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Email verified successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthApiResponseSchemas.Empty.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "code": 200,
                                              "message": "Email verified successfully.",
                                              "data": null
                                            }
                                            """)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid request, invalid OTP, or OTP expired",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "status": 400,
                                              "message": "Invalid OTP",
                                              "timestamp": "2026-04-08T11:30:00"
                                            }
                                            """)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "User not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject("""
                                            {
                                                "status": 404,
                                                "message": "User not found",
                                                "timestamp": "2026-04-08T11:30:00"
                                            }
                                            """)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "429",
                            description = "Too many OTP verification attempts",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject("""
                                            {
                                                "status": 429,
                                                "message": "Too many OTP verification attempts. Please try again later.",
                                                "timestamp": "2026-04-08T11:30:00"
                                            }
                                            """)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "500",
                            description = "Unexpected server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                                "status": 500,
                                                "message": "Unexpected server error",
                                                "timestamp": "2026-04-08T11:30:00"
                                            }
                                            """)
                            )
                    )
            }
    )
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        authService.verifyEmail(request);

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
            description = "Authenticate with email/password. Returns Access Token in JSON body and sets Refresh Token as HttpOnly cookie.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Login successful",
                            headers = {
                                    @io.swagger.v3.oas.annotations.headers.Header(
                                            name = HttpHeaders.SET_COOKIE,
                                            description = "HttpOnly Refresh Token cookie (sp_refresh_token)"
                                    )
                            },
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthApiResponseSchemas.Login.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "code": 200,
                                              "message": "Login successful.",
                                              "data": {
                                                "accessToken": "eyJhbGci...",
                                                "tokenType": "Bearer",
                                                "expiresIn": 900000
                                              }
                                            }
                                            """)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Validation failed",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "status": 400,
                                              "message": "Validation failed: email must be a well-formed email address",
                                              "timestamp": "2026-04-08T11:30:00"
                                            }
                                            """)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = "Account is not verified",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "status": 403,
                                              "message": "Account is not verified. Please verify your email before logging in.",
                                              "timestamp": "2026-04-08T11:30:00"
                                            }
                                            """)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Invalid username or password",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "status": 404,
                                              "message": "Invalid username or password",
                                              "timestamp": "2026-04-08T11:30:00"
                                            }
                                            """)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "423",
                            description = "Account is locked",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "status": 423,
                                              "message": "Account is locked due to too many failed login attempts. Please try again later.",
                                              "timestamp": "2026-04-08T11:30:00"
                                            }
                                            """)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "500",
                            description = "Unexpected server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                                "status": 500,
                                                "message": "Unexpected server error",
                                                "timestamp": "2026-04-08T11:30:00"
                                            }
                                            """)
                            )
                    )
            }
    )
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenPair tokens = authService.login(request);
        long accessExpiresInMs = jwtService.getJwtProperties().getExpirationMs();
        long refreshExpiresInMs = jwtService.getJwtProperties().getRefreshExpirationMs();

        // Refresh Token → HttpOnly cookie
        // Fe can not read, only sends to server when /refresh
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, tokens.refreshToken())
                .httpOnly(true)
                .secure(false) // TODO: set true khi deploy HTTPS
                .path("/api/v1/auth/refresh")  // Chỉ gửi cookie lên /refresh endpoint
                .sameSite("Lax")
                .maxAge(refreshExpiresInMs / 1000)
                .build();

        // Access Token → JSON body
        // store in memory and send with Authorization header for subsequent requests
        LoginResponse result = LoginResponse.builder()
                .accessToken(tokens.accessToken())
                .tokenType("Bearer")
                .expiresIn(accessExpiresInMs)
                .build();

        ApiResponse<LoginResponse> response = ApiResponse.<LoginResponse>builder()
                .code(200)
                .message("Login successful.")
                .data(result)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }

    // new access token from refresh token in HttpOnly cookie
    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh Access Token",
            description = "Reads sp_refresh_token HttpOnly cookie and issues a new short-lived Access Token in the JSON body.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Access Token refreshed successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = """
                                            {
                                              "code": 200,
                                              "message": "Token refreshed.",
                                              "data": {
                                                "accessToken": "eyJhbGci...",
                                                "tokenType": "Bearer",
                                                "expiresIn": 900000
                                              }
                                            }
                                            """)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "Missing, invalid, or expired Refresh Token",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "status": 401,
                                              "message": "Invalid or expired refresh token",
                                              "timestamp": "2026-04-09T00:00:00"
                                            }
                                            """)
                            )
                    )
            }
    )
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request);

        String newAccessToken = refreshTokenService.rotateAccessToken(refreshToken);
        long accessExpiresInMs = jwtService.getJwtProperties().getExpirationMs();

        LoginResponse result = LoginResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(accessExpiresInMs)
                .build();

        ApiResponse<LoginResponse> response = ApiResponse.<LoginResponse>builder()
                .code(200)
                .message("Token refreshed.")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
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
            description = "Clears the Refresh Token HttpOnly cookie (sp_refresh_token).",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Logout successful",
                            headers = {
                                    @io.swagger.v3.oas.annotations.headers.Header(
                                            name = HttpHeaders.SET_COOKIE,
                                            description = "Clears HttpOnly Refresh Token cookie (sp_refresh_token)"
                                    )
                            },
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthApiResponseSchemas.Empty.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "code": 200,
                                              "message": "Logout successful.",
                                              "data": null
                                            }
                                            """)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "500",
                            description = "Unexpected server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                                "status": 500,
                                                "message": "Unexpected server error",
                                                "timestamp": "2026-04-08T11:30:00"
                                            }
                                            """)
                            )
                    )
            }
    )
    public ResponseEntity<ApiResponse<Void>> logout() {
        // remove refresh token cookie by setting maxAge=0
        ResponseCookie clearRefreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false) // set true when deploy with HTTPS
                .path("/api/v1/auth/refresh")
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

    @GetMapping("/me")
    public ResponseEntity<UserAuthorizedResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails user) {
        UserAuthorizedResponse response = UserAuthorizedResponse.builder()
                .id(user.getUser().getId())
                .email(user.getUser().getEmail())
                .role(user.getUser().getRole())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request password reset",
            description = "Sends an OTP to the user's email to reset their password",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "OTP sent successfully"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Email not found"
                    )
            }
    )
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.processForgotPassword(request);

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
            description = "Resends a new OTP to the user's email",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "New OTP sent successfully"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "429",
                            description = "Too many requests (Cooldown active)"
                    )
            }
    )
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        passwordResetService.processResendOtp(request);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(200)
                .message("Mã OTP mới đã được gửi thành công.")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password",
            description = "Verifies the OTP and updates the user's password",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Password reset successfully"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid or expired OTP"
                    )
            }
    )
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.processResetPassword(request);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(200)
                .message("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới.")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }
}
