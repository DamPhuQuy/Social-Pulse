package com.socialpulse.app.auth.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cấu hình Spring Security.
 *
 * Các thay đổi so với version cũ:
 * 1. SessionCreationPolicy.STATELESS — API không dùng session, mỗi request phải mang JWT
 * 2. addFilterBefore(jwtFilter) — thêm JWT filter trước filter xử lý username/password
 * 3. AuthenticationManager bean — cho AuthService dùng để authenticate
 * 4. Fix whitelist: "/api/auth/**" → "/api/v1/auth/**" (bug cũ: prefix sai)
 *
 * Tại sao không cần khai báo DaoAuthenticationProvider thủ công?
 * → Spring Security tự detect PasswordEncoder bean (implements Spring interface)
 *   và CustomUserDetailsService bean (implements UserDetailsService),
 *   rồi tự wire chúng vào DaoAuthenticationProvider.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // JwtAuthenticationFilter được inject để thêm vào filter chain
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // STATELESS: không tạo/dùng HttpSession — bắt buộc cho REST API với JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // FIX BUG: path cũ "/api/auth/**" sai, đúng là "/api/v1/auth/**"
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // JWT filter chạy TRƯỚC UsernamePasswordAuthenticationFilter
                // để set Authentication vào SecurityContext từ token
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Expose AuthenticationManager để AuthService.login() gọi authenticate().
     * AuthenticationConfiguration tự build manager từ các bean có sẵn:
     * → CustomUserDetailsService + PasswordEncoder → DaoAuthenticationProvider
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
