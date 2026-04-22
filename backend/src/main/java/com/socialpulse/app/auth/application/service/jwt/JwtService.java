package com.socialpulse.app.auth.application.service.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;

import com.socialpulse.app.auth.application.usecase.JwtUseCase;
import com.socialpulse.app.security.jwt.JwtProperties;
import com.socialpulse.app.security.user.CustomUserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;

/**
 * jwt structure: header.payload.signature
 *   - Header: HS256
 *   - Payload: email (subject), userId, role, iat, exp
 *   - Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
 */
@Getter
public class JwtService implements JwtUseCase {

    private JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    // inject userId and role into claims to let frontend use
    @Override
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
    @Override
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
    @Override
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    @Override
    public long getAccessExpirationMs() {
        return jwtProperties.getExpirationMs();
    }

    @Override
    public long getRefreshExpirationMs() {
        return jwtProperties.getRefreshExpirationMs();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}

