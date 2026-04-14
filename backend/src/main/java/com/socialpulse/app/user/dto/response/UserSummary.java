package com.socialpulse.app.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserSummary {
    private Long id;
    private String username;
    private String avatarUrl;
}
