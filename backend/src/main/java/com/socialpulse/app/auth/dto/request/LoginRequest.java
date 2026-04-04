package com.socialpulse.app.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * Request body cho POST /api/v1/auth/login.
 *
 * Dùng @NotBlank thay vì @NotNull vì blank string ("  ")
 * vẫn pass @NotNull nhưng sẽ fail authentication.
 * @Email validate format cơ bản của email.
 */
@Getter
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
