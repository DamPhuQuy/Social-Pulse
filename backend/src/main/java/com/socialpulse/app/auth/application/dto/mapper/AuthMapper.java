package com.socialpulse.app.auth.application.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.auth.application.dto.TokenPair;
import com.socialpulse.app.auth.application.dto.response.LoginResponse;
import com.socialpulse.app.user.application.dto.response.UserAuthorizedResponse;
import com.socialpulse.app.user.domain.model.User;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    TokenPair toTokenPair(String accessToken, String refreshToken);

    @Mapping(target = "accessToken", source = "tokens.accessToken")
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "expiresIn", source = "accessExpiresInMs")
    LoginResponse toLoginResponse(TokenPair tokens, long accessExpiresInMs);

    UserAuthorizedResponse toUserAuthorizedResponse(User user);
}
