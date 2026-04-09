package com.socialpulse.app.auth.security.encoder;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Wrapper quanh BCryptPasswordEncoder.
 *
 * Tại sao implement Spring Security's PasswordEncoder?
 * → DaoAuthenticationProvider cần một bean implements interface này
 *   để verify password khi authenticate(). Bằng cách implement interface,
 *   Spring Security tự động detect và wire vào AuthenticationManager.
 *
 * Cost factor 12: cân bằng giữa security và performance
 * (mỗi lần hash mất ~300ms trên máy thông thường).
 */
@Component
public class PasswordEncoder implements org.springframework.security.crypto.password.PasswordEncoder {

    private final BCryptPasswordEncoder encoder;

    public PasswordEncoder() {
        encoder = new BCryptPasswordEncoder(12);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
