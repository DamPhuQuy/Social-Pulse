package com.socialpulse.app.user.application.dto.response;

import java.time.LocalDate;

import com.socialpulse.app.user.domain.enums.UserGender;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserViewProfileResponse {
    private Long userId;
    private String username;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private LocalDate dob;
    private UserGender gender;
    private long postCount;
    private long followers;
    private long following;
    private boolean isFollowing;
}
