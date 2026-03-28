package com.socialpulse.app.common.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoder {
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public static String encode(String rawPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
        return passwordEncoder.encode(rawPassword);
    }
}
