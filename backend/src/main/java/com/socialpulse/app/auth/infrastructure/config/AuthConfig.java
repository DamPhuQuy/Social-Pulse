package com.socialpulse.app.auth.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;

import com.socialpulse.app.auth.adapter.out.EmailAdapter;
import com.socialpulse.app.auth.adapter.out.OtpStorageAdapter;
import com.socialpulse.app.auth.adapter.out.RefreshTokenRepositoryAdapter;
import com.socialpulse.app.auth.application.dto.mapper.AuthMapper;
import com.socialpulse.app.auth.application.port.in.JwtUseCase;
import com.socialpulse.app.auth.application.port.in.LoginUseCase;
import com.socialpulse.app.auth.application.port.in.OtpUseCase;
import com.socialpulse.app.auth.application.port.in.PasswordResetUseCase;
import com.socialpulse.app.auth.application.port.in.RefreshTokenRevocationUseCase;
import com.socialpulse.app.auth.application.port.in.RefreshTokenUseCase;
import com.socialpulse.app.auth.application.port.in.RegisterUseCase;
import com.socialpulse.app.auth.application.port.in.VerifyEmailUseCase;
import com.socialpulse.app.auth.application.port.out.EmailPort;
import com.socialpulse.app.auth.application.port.out.OtpStoragePort;
import com.socialpulse.app.auth.application.port.out.RefreshTokenRepositoryPort;
import com.socialpulse.app.auth.application.service.LoginService;
import com.socialpulse.app.auth.application.service.RegisterService;
import com.socialpulse.app.auth.application.service.VerifyEmailService;
import com.socialpulse.app.auth.application.service.jwt.JwtService;
import com.socialpulse.app.auth.application.service.jwt.RefreshTokenRevocationService;
import com.socialpulse.app.auth.application.service.jwt.RefreshTokenService;
import com.socialpulse.app.auth.application.service.otp.OtpService;
import com.socialpulse.app.auth.application.service.password.PasswordResetService;
import com.socialpulse.app.auth.infrastructure.persistence.mapper.RefreshTokenMapper;
import com.socialpulse.app.auth.infrastructure.persistence.repository.JpaRefreshTokenRepository;
import com.socialpulse.app.auth.security.encoder.AppPasswordEncoder;
import com.socialpulse.app.auth.security.jwt.JwtProperties;
import com.socialpulse.app.user.application.port.in.CreateUserUseCase;
import com.socialpulse.app.user.application.port.out.UserRepositoryPort;

@Configuration
public class AuthConfig {

    // Adapters ---------------------------------------------

    @Bean
    public RefreshTokenRepositoryPort refreshTokenRepositoryPort(JpaRefreshTokenRepository jpaRefreshTokenRepository,
                                                                 RefreshTokenMapper refreshTokenMapper) {
        return new RefreshTokenRepositoryAdapter(jpaRefreshTokenRepository, refreshTokenMapper);
    }

    @Bean
    public EmailPort emailPort(JavaMailSender mailSender) {
        return new EmailAdapter(mailSender);
    }

    @Bean
    public OtpStoragePort otpStoragePort(StringRedisTemplate redisTemplate) {
        return new OtpStorageAdapter(redisTemplate);
    }

    // Use Cases ---------------------------------------------

    @Bean
    public RegisterUseCase registerUseCase(CreateUserUseCase createUserUseCase, OtpUseCase otpUseCase) {
        return new RegisterService(createUserUseCase, otpUseCase);
    }

    @Bean
    public VerifyEmailUseCase verifyEmailUseCase(UserRepositoryPort userRepositoryPort, OtpUseCase otpUseCase) {
        return new VerifyEmailService(userRepositoryPort, otpUseCase);
    }

    @Bean
    public LoginUseCase loginUseCase(UserRepositoryPort userRepositoryPort,
                                     AuthenticationManager authenticationManager,
                                     JwtUseCase jwtUseCase,
                                     RefreshTokenUseCase refreshTokenUseCase,
                                     AuthMapper authMapper) {
        return new LoginService(userRepositoryPort, authenticationManager, jwtUseCase, refreshTokenUseCase, authMapper);
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(
        JwtUseCase jwtUseCase,
        RefreshTokenRepositoryPort refreshTokenRepository,
        RefreshTokenRevocationUseCase refreshTokenRevocationUseCase,
        UserRepositoryPort userRepository,
        AuthMapper authMapper) {
        return new RefreshTokenService(jwtUseCase, refreshTokenRepository, refreshTokenRevocationUseCase, userRepository, authMapper);
    }

    @Bean
    public RefreshTokenRevocationUseCase refreshTokenRevocationUseCase(RefreshTokenRepositoryPort refreshTokenRepository) {
        return new RefreshTokenRevocationService(refreshTokenRepository);
    }

    @Bean
    public JwtUseCase jwtUseCase(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    @Bean
    public OtpUseCase otpUseCase(OtpStoragePort otpStoragePort, EmailPort emailPort, AppPasswordEncoder passwordEncoder) {
        return new OtpService(otpStoragePort, emailPort, passwordEncoder);
    }

    @Bean
    public PasswordResetUseCase passwordResetUseCase(UserRepositoryPort userRepositoryPort, AppPasswordEncoder passwordEncoder, OtpUseCase otpUseCase) {
        return new PasswordResetService(userRepositoryPort, passwordEncoder, otpUseCase);
    }

    // utils ---------------------------------------------
    @Bean
    public AppPasswordEncoder appPasswordEncoder() {
        return new AppPasswordEncoder();
    }
}
