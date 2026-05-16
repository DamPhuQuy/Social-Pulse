package com.socialpulse.app.follow.application.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowResponse {
    private Long id;
    private Long followerId;
    private Long followingId;
    private LocalDateTime createdAt;
}
