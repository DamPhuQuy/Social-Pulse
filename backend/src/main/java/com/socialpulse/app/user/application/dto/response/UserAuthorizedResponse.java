package com.socialpulse.app.user.application.dto.response;

import java.util.Set;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserAuthorizedResponse {
    private Long id;
    private String email;
    private Set<String> roles;
}
