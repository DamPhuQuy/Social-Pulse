package com.socialpulse.app.common.dto.response;

import com.socialpulse.app.auth.dto.response.LoginResponse;
import com.socialpulse.app.comment.dto.response.CommentCreationResponse;
import com.socialpulse.app.user.dto.response.UserCreationResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthApiResponseSchemas {

    @Schema(name = "ApiResponseUserCreationResponse")
    public static class UserCreation extends ApiResponse<UserCreationResponse> {
    }

    @Schema(name = "ApiResponseCommentCreationResponse")
    public static class CommentCreation extends ApiResponse<CommentCreationResponse> {
    }

    @Schema(name = "ApiResponseLoginResponse")
    public static class Login extends ApiResponse<LoginResponse> {
    }

    @Schema(name = "ApiResponseVoid")
    public static class Empty extends ApiResponse<Void> {
    }

    @Schema(name = "ApiResponseBoolean")
    public static class Bool extends ApiResponse<Boolean> {
    }
}

