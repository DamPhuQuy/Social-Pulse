package com.socialpulse.app.auth.service;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.auth.dto.request.LoginRequest;
import com.socialpulse.app.auth.dto.response.LoginResponse;
import com.socialpulse.app.auth.dto.request.EmailVerificationRequest;
import com.socialpulse.app.auth.security.CustomUserDetails;
import com.socialpulse.app.auth.security.JwtService;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.status.ErrorCode;
import com.socialpulse.app.user.dto.request.UserCreationRequest;
import com.socialpulse.app.user.dto.response.UserCreationResponse;
import com.socialpulse.app.user.entity.User;
import com.socialpulse.app.user.entity.UserStatus;
import com.socialpulse.app.user.entity.VerificationStatus;
import com.socialpulse.app.user.repository.UserRepository;
import com.socialpulse.app.user.service.UserService;

@Service
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final Logger logger;

    // Giới hạn số lần đăng nhập sai trước khi khóa tài khoản
    private static final int MAX_FAILED_ATTEMPTS = 5;

    public AuthService(UserService userService,
                       UserRepository userRepository,
                       OtpService otpService,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.logger = LoggerFactory.getLogger(AuthService.class);
    }

    @Transactional
    public UserCreationResponse register(UserCreationRequest request) {
        UserCreationResponse response = userService.createUser(request);
        logger.info("User registered with email: {}", request.getEmail());
        otpService.generateToStoreAndSendEmail(request.getEmail());
        return response;
    }

    @Transactional
    public void verifyEmail(EmailVerificationRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        logger.info("Attempting to verify email: {}", normalizedEmail);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.ACTIVE) {
            otpService.invalidateOtp(normalizedEmail);
            logger.info("Email already verified for: {}", normalizedEmail);
            return;
        }

        otpService.verifyOtp(normalizedEmail, request.getOtpCode());
        user.activeAccount();
        user.verifyAccount();
        userRepository.save(user);
        otpService.invalidateOtp(normalizedEmail);
        logger.info("Email verified for: {}", normalizedEmail);
    }

    /**
     * Xử lý đăng nhập và trả JWT access token.
     *
     * Luồng:
     * 1. Kiểm tra user tồn tại + trạng thái tài khoản (trước khi gọi AuthManager
     *    để trả thông báo lỗi rõ ràng hơn)
     * 2. Gọi AuthenticationManager.authenticate() — Spring Security tự verify password
     * 3. Thành công → cập nhật lastLoginAt, tạo JWT
     * 4. Thất bại → tăng failed attempts, có thể lock account
     *
     * noRollbackFor=AppException.class: đảm bảo việc cập nhật
     * failedLoginAttempts được commit dù method ném AppException.
     */
    @Transactional(noRollbackFor = AppException.class)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        // Bước 1: Tìm user — trả INVALID_CREDENTIALS (không phải USER_NOT_FOUND)
        // để tránh lộ thông tin "email này có tồn tại không"
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        // Kiểm tra trạng thái trước khi xác thực password
        if (user.isLocked() || user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (user.getVerification() != VerificationStatus.VERIFIED
                || user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }

        try {
            // Bước 2: Spring Security authenticate
            // → gọi CustomUserDetailsService.loadUserByUsername(email)
            // → gọi PasswordEncoder.matches(rawPassword, hash)
            // → kiểm tra isEnabled(), isAccountNonLocked()
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );

            // Bước 3: Thành công
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Cập nhật tracking (reset failed attempts, ghi thời gian login)
            user.resetFailedLoginAttempts();
            user.updateLastLoginAt();
            userRepository.save(user);

            // Tạo JWT
            String token = jwtService.generateToken(userDetails);
            logger.info("Login successful for: {}", normalizedEmail);

            return LoginResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getExpirationMs())
                    .build();

        } catch (BadCredentialsException e) {
            // Bước 4: Sai password → tăng counter, có thể lock
            handleFailedLoginAttempt(user);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);

        } catch (LockedException e) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);

        } catch (DisabledException e) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
    }

    /**
     * Tăng số lần đăng nhập thất bại.
     * Nếu đạt giới hạn MAX_FAILED_ATTEMPTS → khóa tài khoản.
     */
    private void handleFailedLoginAttempt(User user) {
        user.incrementFailedLoginAttempts();
        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.lockAccount();
            logger.warn("Account locked after {} failed attempts: {}",
                    MAX_FAILED_ATTEMPTS, user.getEmail());
        }
        userRepository.save(user);
    }
}
