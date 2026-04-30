package com.socialpulse.app.auth.application.service.otp;

import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.socialpulse.app.auth.application.usecase.OtpUseCase;
import com.socialpulse.app.auth.application.port.EmailPort;
import com.socialpulse.app.auth.domain.repository.OtpRepository;
import com.socialpulse.app.auth.domain.model.Otp;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.AuthCode;
import com.socialpulse.app.security.encoder.AppPasswordEncoder;

@Service
public class OtpService implements OtpUseCase {

    // static constants
    private static final String OTP_EMAIL_SUBJECT = "Your OTP Code for Social Pulse";
    private static final long OTP_TTL_SECONDS = 300;
    private static final long OTP_MAX_ATTEMPTS = 5;
    private static final String PAYLOAD_SEPARATOR = "|";

    // Dependencies
    private final OtpRepository otpStoragePort;
    private final EmailPort emailPort;
    private final SecureRandom secureRandom;
    private final AppPasswordEncoder passwordEncoder;

    public OtpService(OtpRepository otpStoragePort, EmailPort emailPort, AppPasswordEncoder passwordEncoder) {
        this.otpStoragePort = otpStoragePort;
        this.emailPort = emailPort;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public void generateToStoreAndSendEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        String otpCode = generateOtpCode();
        Otp otp = newOtp(normalizedEmail, otpCode);
        otpStoragePort.save(normalizedEmail, serialize(otp));
        emailPort.sendHtmlEmail(normalizedEmail, OTP_EMAIL_SUBJECT, buildOtpHtml(otpCode));
    }

    @Override
    public void verifyOtp(String email, String otpCode) {
        String normalizedEmail = normalizeEmail(email);
                Otp otp = readOtp(normalizedEmail);

        if (otp == null || otp.isExpired(Instant.now().toEpochMilli())) {
                        otpStoragePort.delete(normalizedEmail);
            throw new AppException(AuthCode.OTP_EXPIRED);
        }

        if (otp.getAttemptCount() >= OTP_MAX_ATTEMPTS) {
            throw new AppException(AuthCode.OTP_TOO_MANY_ATTEMPTS);
        }

        if (!passwordEncoder.matches(otpCode.trim(), otp.getOtpCode())) {
            long updatedAttempts = otp.getAttemptCount() + 1;
                        otp.setAttemptCount(updatedAttempts);
                        otpStoragePort.save(normalizedEmail, serialize(otp));

            if (updatedAttempts >= OTP_MAX_ATTEMPTS) {
                throw new AppException(AuthCode.OTP_TOO_MANY_ATTEMPTS);
            }
            throw new AppException(AuthCode.OTP_INVALID);
        }
    }

    @Override
    public void invalidateOtp(String email) {
                otpStoragePort.delete(normalizeEmail(email));
    }

        private Otp newOtp(String email, String rawCode) {
                return Otp.builder()
                                .email(email)
                                .otpCode(passwordEncoder.encode(rawCode))
                                .expiredAt(Instant.now().plusSeconds(OTP_TTL_SECONDS).toEpochMilli())
                                .attemptCount(0L)
                                .build();
        }

        private Otp readOtp(String email) {
                String payload = otpStoragePort.findByEmail(email);
                if (payload == null || payload.isBlank()) {
            return null;
        }

                String[] parts = payload.split("\\|", -1);
                if (parts.length != 3) {
                        return null;
                }

        return Otp.builder()
                .email(email)
                .otpCode(parts[0])
                .expiredAt(parseLong(parts[1]))
                .attemptCount(parseLong(parts[2]))
                .build();
    }

        private String serialize(Otp otp) {
                return String.join(PAYLOAD_SEPARATOR,
                                otp.getOtpCode(),
                                String.valueOf(otp.getExpiredAt()),
                                String.valueOf(otp.getAttemptCount()));
        }

        private long parseLong(String value) {
                if (value == null || value.isBlank()) {
            return 0L;
        }
                return Long.parseLong(value);
    }

    private String generateOtpCode() {
        int otp = secureRandom.nextInt(1_000_000);
        return String.format("%06d", otp);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String buildOtpHtml(String otpCode) {
        return """
            <table
                width=\"100%%\"
                cellpadding=\"0\"
                cellspacing=\"0\"
                style=\"background: #f7f9fb; padding: 30px 0\"
            >
                <tr>
                    <td align=\"center\">
                        <table
                            width=\"600\"
                            cellpadding=\"0\"
                            cellspacing=\"0\"
                            style=\"
                                background: #ffffff;
                                border-radius: 10px;
                                padding: 30px;
                                font-family: Arial, sans-serif;
                            \"
                        >
                            <tr>
                                <td align=\"center\">
                                    <h2 style=\"color: #515f74; margin-bottom: 10px\">
                                        Verify Your Account
                                    </h2>
                                    <p style=\"color: #506076; margin: 0\">
                                        Use the OTP below to continue
                                    </p>
                                </td>
                            </tr>

                            <tr>
                                <td style=\"padding: 20px 0\">
                                    <hr style=\"border: none; border-top: 1px solid #eee\" />
                                </td>
                            </tr>

                            <tr>
                                <td align=\"center\">
                                    <div
                                        style=\"
                                            font-size: 36px;
                                            font-weight: bold;
                                            letter-spacing: 6px;
                                            color: #515f74;
                                        \"
                                    >
                                        %s
                                    </div>
                                </td>
                            </tr>

                            <tr>
                                <td style=\"padding-top: 20px; color: #333\">
                                    <p>This code will expire in <b>5 minutes</b>.</p>
                                    <p>If you did not request this, you can safely ignore this email.</p>
                                </td>
                            </tr>

                            <tr>
                                <td
                                    style=\"
                                        padding-top: 30px;
                                        font-size: 12px;
                                        color: #999;
                                        text-align: center;
                                    \"
                                >
                                    © 2026 Social Pulse. All rights reserved.
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        """.formatted(otpCode);
    }
}


