package com.socialpulse.app.auth.security.jwt;

import com.socialpulse.app.auth.service.user.CustomUserDetailsService;
import com.socialpulse.app.auth.service.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Đọc Access Token từ Authorization: Bearer header.
 *
 * Hybrid Auth Pattern:
 *   - Access Token đến qua header (FE gắn vào request từ in-memory state)
 *   - Refresh Token ở trong HttpOnly cookie, chỉ được đọc tại /auth/refresh endpoint
 *
 * Cookie sp_refresh_token KHÔNG được đọc ở đây để tránh CSRF.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String jwt = resolveToken(request);

        // Không có token → tiếp tục chain (Spring Security sẽ chặn nếu endpoint yêu cầu auth)
        if (jwt == null || jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String email = jwtService.extractEmail(jwt);

            // Chỉ set authentication nếu chưa có trong SecurityContext
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token invalid/expired → tiếp tục chain không auth; Spring Security sẽ xử lý 401
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Chỉ đọc Access Token từ Authorization: Bearer header.
     * Cookie Refresh Token không được đọc ở đây.
     */
    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }
}
