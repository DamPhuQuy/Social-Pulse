package com.socialpulse.app.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response trả về sau khi login thành công (Hybrid Auth Pattern).
 *
 * accessToken: Access Token ngắn hạn (15 phút) — FE giữ trong memory.
 * tokenType:   luôn là "Bearer" theo OAuth2 convention.
 * expiresIn:   thời gian sống của Access Token tính bằng milliseconds (15 phút = 900000).
 *
 * Refresh Token được set vào HttpOnly cookie (sp_refresh_token) — không xuất hiện ở đây.
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

