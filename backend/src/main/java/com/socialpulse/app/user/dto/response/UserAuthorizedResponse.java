package com.socialpulse.app.user.dto.response;

import com.socialpulse.app.user.entity.UserRole;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserAuthorizedResponse {
    private Long id;
    private String email;
    private UserRole role;
}
