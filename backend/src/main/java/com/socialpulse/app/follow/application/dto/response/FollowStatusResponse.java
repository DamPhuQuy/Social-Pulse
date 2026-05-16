package com.socialpulse.app.follow.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowStatusResponse {
    private Long targetUserId;
    private boolean following;
}
