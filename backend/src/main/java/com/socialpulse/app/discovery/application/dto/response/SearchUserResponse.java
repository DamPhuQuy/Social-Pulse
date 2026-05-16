package com.socialpulse.app.discovery.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchUserResponse {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
}
