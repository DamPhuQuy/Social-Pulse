package com.socialpulse.app.user.application.dto.request;

import java.time.LocalDate;

import com.socialpulse.app.user.domain.enums.UserGender;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileMutationRequest {
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]*$", message = "Username must contain only letters, numbers, and underscores")
    private String username;

    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    private LocalDate dob;

    private UserGender gender;

    @Size(max = 2048, message = "Avatar URL must not exceed 2048 characters")
    private String avatarUrl;

    @Size(max = 255, message = "Avatar public ID must not exceed 255 characters")
    private String avatarPublicId;

    @Size(max = 2048, message = "Cover image URL must not exceed 2048 characters")
    private String coverImageUrl;

    @Size(max = 255, message = "Cover image public ID must not exceed 255 characters")
    private String coverImagePublicId;
}
