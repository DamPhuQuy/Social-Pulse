package com.socialpulse.app.auth.domain.repository;

public interface OtpRepository {
    void save(String email, String code);

    String findByEmail(String email);

    void delete(String email);
}

