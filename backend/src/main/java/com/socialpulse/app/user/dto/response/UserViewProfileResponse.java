package com.socialpulse.app.user.dto.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserViewProfileResponse {
    private Long userId;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private LocalDate dob;
}
