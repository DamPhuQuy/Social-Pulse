package com.socialpulse.app.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Otp {
    private String email;
    private String otpCode;
    private Long expiredAt;
    private Long attemptCount;

    public boolean isExpired(long nowMillis) {
        return expiredAt == null || expiredAt < nowMillis;
    }
}
