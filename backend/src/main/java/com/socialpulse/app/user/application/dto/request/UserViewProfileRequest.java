package com.socialpulse.app.user.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserViewProfileRequest {
    @NotNull(message = "Target user ID must not be null")
    @Positive(message = "Target user ID must be greater than 0")
    private Long targetUserId;
}
