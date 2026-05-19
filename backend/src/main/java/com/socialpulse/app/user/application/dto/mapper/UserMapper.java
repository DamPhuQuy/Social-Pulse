package com.socialpulse.app.user.application.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.user.application.dto.request.UserCreationRequest;
import com.socialpulse.app.user.application.dto.response.UserCreationResponse;
import com.socialpulse.app.user.application.dto.response.UserSummary;
import com.socialpulse.app.user.domain.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "email", source = "normalizedEmail")
    @Mapping(target = "passwordHash", source = "encodedPassword")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "verification", constant = "NOT_VERIFIED")
    @Mapping(target = "isLocked", ignore = true)
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toUser(UserCreationRequest request, String normalizedEmail, String encodedPassword);

    @Mapping(target = "message", source = "message")
    UserCreationResponse toUserCreationResponse(User user, String message);



    @Mapping(target = "avatarUrl", source = "profile.avatarUrl")
    UserSummary toUserSummary(User user);
}
