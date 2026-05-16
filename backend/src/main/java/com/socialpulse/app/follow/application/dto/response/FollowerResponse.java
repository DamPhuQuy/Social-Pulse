package com.socialpulse.app.follow.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowerResponse {
    private Long userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private Boolean isFollowing;
}
