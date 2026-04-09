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
import java.util.UUID;
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

    // inject userId and role into claims to let frontend use
    public String generateToken(CustomUserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        extraClaims.put("userId", userDetails.getId());
        extraClaims.put("role", userDetails.getUser().getRole().name());
        extraClaims.put("type", "access");

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claims(extraClaims)
                .subject(userDetails.getUsername()) // sub
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .issuer("social-pulse-api")
                .compact();
    }

    /**
     * Refresh Token — payload gọn (chỉ sub + type=refresh).
     * TTL lấy từ refreshExpirationMs (default: 7 ngày).
     */
    public String generateRefreshToken(CustomUserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("type", "refresh");

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshExpirationMs());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .issuer("social-pulse-api")
                .compact();
    }

    // create hmac from the secret key string
    // hmacShaKeyFor automatically choose the HMAC algorithm based on the key length (HS256, HS384, HS512)
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    // parse and verify, throw exception if there are errors
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // extract email and expiration from jwt
    // signature and expiration is verified in the process of extracting
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
