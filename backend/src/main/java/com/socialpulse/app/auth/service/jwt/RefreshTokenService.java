package com.socialpulse.app.auth.service.jwt;

import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.auth.service.user.CustomUserDetailsService;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.status.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Xử lý việc làm mới Access Token từ Refresh Token (stateless, không lưu DB).
 *
 * Flow:
 *   FE gọi POST /auth/refresh (kèm cookie sp_refresh_token)
 *   → Backend parse + verify Refresh Token
 *   → Kiểm tra claim type="refresh"
 *   → Load user → phát Access Token mới
 */
@Service
public class RefreshTokenService {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public RefreshTokenService(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Validate Refresh Token và phát Access Token mới.
     *
     * @param refreshToken chuỗi JWT lấy từ cookie sp_refresh_token
     * @return Access Token mới (ngắn hạn, 15 phút)
     * @throws AppException nếu token không hợp lệ, hết hạn, hoặc không phải loại refresh
     */
    public String rotateAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        try {
            // Parse và verify chữ ký + expiry
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            // Guard: phải là refresh token, không phải access token
            String tokenType = claims.get("type", String.class);
            if (!"refresh".equals(tokenType)) {
                throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
            }

            String email = claims.getSubject();
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);

            return jwtService.generateToken(userDetails);

        } catch (AppException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtService.getJwtProperties().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
