package com.socialpulse.app.user.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserCreationResponse {
    private Long id;
    private String username;
    private String email;
    private String message;
}
