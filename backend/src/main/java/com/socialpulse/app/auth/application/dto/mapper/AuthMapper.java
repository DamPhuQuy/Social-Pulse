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

    @Mapping(target = "roles", expression = "java(user.getRoles() == null ? java.util.Collections.emptySet() : user.getRoles().stream().filter(java.util.Objects::nonNull).map(role -> role.getName()).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet()))")
    UserAuthorizedResponse toUserAuthorizedResponse(User user);
}
