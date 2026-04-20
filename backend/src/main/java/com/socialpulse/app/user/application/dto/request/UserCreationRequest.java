package com.socialpulse.app.user.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class UserCreationRequest {
    @NotBlank
    @Size(min = 6, max = 20, message = "Username must be between 6 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Username must be alphanumeric")
    @Schema(example = "phuquy123", description = "Username must be between 6 and 20 characters, alphanumeric only")
    private String username;

    @NotBlank
    @Email(message = "Email should be valid")
    @Schema(example = "phuquydam06@gmail.com", description = "Email must be a valid email address")
    private String email;

    @NotBlank
    @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    @Schema(example = "P@ssw0rd", description = "Password must be between 6 and 20 characters, contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    private String rawPassword;

    @NotBlank
    @Size(min = 6, max = 20, message = "Confirm password must be between 6 and 20 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", message = "Confirm password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    @Schema(example = "P@ssw0rd", description = "Confirm password must be between 6 and 20 characters, contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    private String confirmPassword;
}
