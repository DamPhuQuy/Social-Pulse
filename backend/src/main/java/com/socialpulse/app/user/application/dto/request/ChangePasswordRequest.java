package com.socialpulse.app.user.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class ChangePasswordRequest {
    @NotBlank(message = "Current password cannot be blank")
    @Schema(example = "OldP@ssw0rd", description = "Current password")
    private String currentPassword;

    @NotBlank(message = "New password cannot be blank")
    @Size(min = 6, max = 20, message = "New password must be between 6 and 20 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
             message = "New password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    @Schema(example = "NewP@ssw0rd", description = "New password must be between 6 and 20 characters, contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    private String newPassword;

    @NotBlank(message = "Confirm password cannot be blank")
    @Size(min = 6, max = 20, message = "Confirm password must be between 6 and 20 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
             message = "Confirm password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    @Schema(example = "NewP@ssw0rd", description = "Confirm password must match new password")
    private String confirmPassword;
}
