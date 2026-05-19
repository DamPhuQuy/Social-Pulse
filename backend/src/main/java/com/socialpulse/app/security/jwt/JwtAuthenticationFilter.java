package com.socialpulse.app.security.jwt;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.socialpulse.app.auth.application.usecase.JwtUseCase;
import com.socialpulse.app.security.user.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUseCase jwtUseCase;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUseCase jwtUseCase, CustomUserDetailsService userDetailsService) {
        this.jwtUseCase = jwtUseCase;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String jwt = resolveToken(request);

        // Không có token → tiếp tục chain (Spring Security sẽ chặn nếu endpoint yêu cầu auth)
        if (jwt == null || jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String email = jwtUseCase.extractEmail(jwt);

            // Chỉ set authentication nếu chưa có trong SecurityContext
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtUseCase.isTokenValid(jwt, userDetails)) {
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
            log.warn("[JWT Filter] Token validation failed for request [{}]: {} - {}",
                    request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Chỉ đọc Access Token từ Authorization: Bearer header.
     * Hỗ trợ đọc từ query parameter 'token' cho kết nối SSE.
     */
    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isBlank()) {
            return tokenParam.trim();
        }
        return null;
    }
}

