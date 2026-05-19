package com.socialpulse.app.auth.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;

import com.socialpulse.app.auth.adapter.persistence.EmailAdapter;
import com.socialpulse.app.auth.adapter.persistence.OtpStorageAdapter;
import com.socialpulse.app.auth.adapter.persistence.RefreshTokenRepositoryAdapter;
import com.socialpulse.app.auth.application.dto.mapper.AuthMapper;
import com.socialpulse.app.auth.application.usecase.JwtUseCase;
import com.socialpulse.app.auth.application.usecase.AuthenticationUseCase;
import com.socialpulse.app.auth.application.usecase.OtpUseCase;
import com.socialpulse.app.auth.application.usecase.PasswordResetUseCase;
import com.socialpulse.app.auth.application.usecase.RefreshTokenRevocationUseCase;
import com.socialpulse.app.auth.application.usecase.RefreshTokenUseCase;
import com.socialpulse.app.auth.application.usecase.RegisterUseCase;
import com.socialpulse.app.auth.application.usecase.VerifyEmailUseCase;
import com.socialpulse.app.auth.application.port.EmailPort;
import com.socialpulse.app.auth.domain.repository.OtpRepository;
import com.socialpulse.app.auth.domain.repository.RefreshTokenRepository;
import com.socialpulse.app.auth.application.service.AuthenticationService;
import com.socialpulse.app.auth.application.service.RegisterService;
import com.socialpulse.app.auth.application.service.VerifyEmailService;
import com.socialpulse.app.auth.application.service.jwt.JwtService;
import com.socialpulse.app.auth.application.service.jwt.RefreshTokenRevocationService;
import com.socialpulse.app.auth.application.service.jwt.RefreshTokenService;
import com.socialpulse.app.auth.application.service.otp.OtpService;
import com.socialpulse.app.auth.application.service.password.PasswordResetService;
import com.socialpulse.app.auth.infrastructure.persistence.mapper.RefreshTokenMapper;
import com.socialpulse.app.auth.infrastructure.persistence.repository.JpaRefreshTokenRepository;
import com.socialpulse.app.security.encoder.AppPasswordEncoder;
import com.socialpulse.app.security.jwt.JwtProperties;
import com.socialpulse.app.user.application.usecase.CreateUserUseCase;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Configuration
public class AuthConfig {

    // Adapters ---------------------------------------------

    @Bean
    public RefreshTokenRepository refreshTokenRepositoryPort(JpaRefreshTokenRepository jpaRefreshTokenRepository,
                                                                 RefreshTokenMapper refreshTokenMapper) {
        return new RefreshTokenRepositoryAdapter(jpaRefreshTokenRepository, refreshTokenMapper);
    }

    @Bean
    public EmailPort emailPort(JavaMailSender mailSender) {
        return new EmailAdapter(mailSender);
    }

    @Bean
    public OtpRepository otpStoragePort(StringRedisTemplate redisTemplate) {
        return new OtpStorageAdapter(redisTemplate);
    }

    // Use Cases ---------------------------------------------

    @Bean
    public RegisterUseCase registerUseCase(CreateUserUseCase createUserUseCase, OtpUseCase otpUseCase) {
        return new RegisterService(createUserUseCase, otpUseCase);
    }

    @Bean
    public VerifyEmailUseCase verifyEmailUseCase(UserRepository userRepositoryPort, OtpUseCase otpUseCase) {
        return new VerifyEmailService(userRepositoryPort, otpUseCase);
    }

    @Bean
    public AuthenticationUseCase loginUseCase(UserRepository userRepositoryPort,
                                              AuthenticationManager authenticationManager,
                                              JwtUseCase jwtUseCase,
                                              RefreshTokenUseCase refreshTokenUseCase,
                                              AuthMapper authMapper) {
        return new AuthenticationService(userRepositoryPort, authenticationManager, jwtUseCase, refreshTokenUseCase, authMapper);
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(
        JwtUseCase jwtUseCase,
        RefreshTokenRepository refreshTokenRepository,
        RefreshTokenRevocationUseCase refreshTokenRevocationUseCase,
        UserRepository userRepository,
        AuthMapper authMapper) {
        return new RefreshTokenService(jwtUseCase, refreshTokenRepository, refreshTokenRevocationUseCase, userRepository, authMapper);
    }

    @Bean
    public RefreshTokenRevocationUseCase refreshTokenRevocationUseCase(RefreshTokenRepository refreshTokenRepository) {
        return new RefreshTokenRevocationService(refreshTokenRepository);
    }

    @Bean
    public JwtUseCase jwtUseCase(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    @Bean
    public OtpUseCase otpUseCase(OtpRepository otpStoragePort, EmailPort emailPort, AppPasswordEncoder passwordEncoder, org.springframework.core.env.Environment environment) {
        return new OtpService(otpStoragePort, emailPort, passwordEncoder, environment);
    }

    @Bean
    public PasswordResetUseCase passwordResetUseCase(UserRepository userRepositoryPort, AppPasswordEncoder passwordEncoder, OtpUseCase otpUseCase) {
        return new PasswordResetService(userRepositoryPort, passwordEncoder, otpUseCase);
    }

    // utils ---------------------------------------------
    @Bean
    public AppPasswordEncoder appPasswordEncoder() {
        return new AppPasswordEncoder();
    }
}



