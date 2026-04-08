package com.socialpulse.app.auth.service.jwt;

import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.auth.security.jwt.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * jwt structure: header.payload.signature
 *   - Header: HS256
 *   - Payload: email (subject), userId, role, iat, exp
 *   - Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
 */
@Service
@Getter
public class JwtService {

    private JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

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
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpirationMs()))
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

    // create hmac from the secret key string
    // hmacShaKeyFor automatically choose the HMAC algorithm based on the key length (HS256, HS384, HS512)
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
