package com.socialpulse.app.auth.security;

import com.socialpulse.app.auth.service.CustomUserDetailsService;
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
 * Filter chạy MỘT LẦN mỗi request (OncePerRequestFilter).
 *
 * Luồng xử lý:
 * 1. Đọc header "Authorization: Bearer <token>"
 * 2. Nếu không có → bỏ qua (request tiếp tục đến Spring Security)
 * 3. Nếu có → extract email từ JWT
 * 4. Load UserDetails từ DB
 * 5. Verify token (chữ ký + expiry + email khớp)
 * 6. Set Authentication vào SecurityContext → Spring Security biết user đã auth
 *
 * Lưu ý: Filter KHÔNG ném exception khi token sai. Nó chỉ NOT set
 * authentication. Spring Security sẽ tự trả 401 nếu route yêu cầu auth.
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

        final String authHeader = request.getHeader("Authorization");

        // Không có header hoặc không phải Bearer → bỏ qua, đến filter tiếp theo
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Cắt "Bearer " (7 ký tự) để lấy token thuần
        final String jwt = authHeader.substring(7);

        try {
            final String email = jwtService.extractEmail(jwt);

            // Chỉ xử lý nếu có email VÀ chưa có authentication trong context
            // (tránh xử lý lại nếu filter chain đã set authentication trước đó)
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load user từ DB — nếu user bị xóa sau khi token được cấp,
                // UsernameNotFoundException sẽ được ném → token không hợp lệ
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Tạo Authentication object với authorities (roles/permissions)
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null, // credentials = null vì đã xác thực qua JWT
                                    userDetails.getAuthorities()
                            );
                    // Gắn thêm request info (IP, session) vào authentication
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // ĐẶT VÀO SECURITY CONTEXT — từ đây Spring Security biết user đã auth
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token sai chữ ký, hết hạn, hoặc user không tồn tại
            // → không set authentication → Spring Security tự trả 401
            // Không ném exception để tránh bypass các filter còn lại
        }

        filterChain.doFilter(request, response);
    }
}
