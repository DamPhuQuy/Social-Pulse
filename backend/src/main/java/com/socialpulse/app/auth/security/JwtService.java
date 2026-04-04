package com.socialpulse.app.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Chịu trách nhiệm toàn bộ việc tạo và xác thực JWT.
 *
 * Cấu trúc JWT gồm 3 phần: header.payload.signature
 *   - Header: thuật toán ký (HS256)
 *   - Payload (claims): subject=email, userId, role, iat, exp
 *   - Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
 */
@Service
public class JwtService {

    // Đọc từ app.jwt.secret trong application-dev.yaml
    @Value("${app.jwt.secret}")
    private String secret;

    // Đọc từ app.jwt.expiration-ms (86400000 = 24h)
    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Tạo JWT cho user đã xác thực thành công.
     * Nhúng thêm userId và role vào claims để frontend dùng
     * mà không cần gọi thêm API /me.
     */
    public String generateToken(CustomUserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userDetails.getId());
        // Lấy role đầu tiên (ROLE_USER / ROLE_ADMIN)
        extraClaims.put("role", userDetails.getUser().getRole());

        return Jwts.builder()
                .claims(extraClaims)
                // subject = email — đây là "username" trong Spring Security context
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                // signWith tự chọn HS256 khi key là SecretKey HMAC
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Trích xuất email từ JWT (không verify signature).
     * Chỉ dùng để biết tìm user nào, verify ở bước tiếp theo.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Kiểm tra token có hợp lệ không:
     * 1. Email trong token phải khớp với email của userDetails
     * 2. Token chưa hết hạn
     *
     * Signature đã được verify tự động bên trong extractAllClaims()
     * — nếu sai chữ ký, JwtException sẽ được ném.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    /**
     * Parse và verify JWT.
     * Nếu signature sai hoặc token bị tamper → JwsException (runtime).
     * Nếu token hết hạn → ExpiredJwtException (extends JwtException).
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Tạo HMAC-SHA256 signing key từ secret string.
     * Keys.hmacShaKeyFor() tự chọn HS256/HS384/HS512 dựa vào độ dài key.
     * Secret >= 32 bytes → HS256; >= 48 → HS384; >= 64 → HS512.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
