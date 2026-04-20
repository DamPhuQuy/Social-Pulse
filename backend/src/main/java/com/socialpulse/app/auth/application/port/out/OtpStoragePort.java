package com.socialpulse.app.auth.application.port.out;

public interface OtpStoragePort {
    void save(String email, String code);

    String findByEmail(String email);

    void delete(String email);
}
