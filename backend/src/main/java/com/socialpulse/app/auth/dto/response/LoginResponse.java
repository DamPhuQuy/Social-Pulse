package com.socialpulse.app.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response trả về sau khi login thành công.
 *
 * accessToken: JWT token, client lưu và gửi kèm mọi request protected
 *              trong header: "Authorization: Bearer <accessToken>"
 * tokenType:   luôn là "Bearer" theo OAuth2 convention
 * expiresIn:   thời gian sống của token tính bằng milliseconds (24h = 86400000)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
}
