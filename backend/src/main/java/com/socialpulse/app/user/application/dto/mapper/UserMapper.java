package com.socialpulse.app.user.application.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.user.application.dto.request.UserCreationRequest;
import com.socialpulse.app.user.application.dto.response.AdminUserResponse;
import com.socialpulse.app.user.application.dto.response.UserCreationResponse;
import com.socialpulse.app.user.application.dto.response.UserSummary;
import com.socialpulse.app.user.application.dto.response.UserViewProfileResponse;
import com.socialpulse.app.user.domain.model.Role;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.model.UserProfile;

import java.util.Set;
import java.util.stream.Collectors;

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
    @Mapping(target = "topics", ignore = true)
    User toUser(UserCreationRequest request, String normalizedEmail, String encodedPassword);

    @Mapping(target = "message", source = "message")
    UserCreationResponse toUserCreationResponse(User user, String message);

    @Mapping(target = "userId", source = "id")
    UserViewProfileResponse toUserViewProfileResponse(UserProfile userProfile);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "displayName", source = "profile.displayName")
    @Mapping(target = "bio", source = "profile.bio")
    @Mapping(target = "avatarUrl", source = "profile.avatarUrl")
    @Mapping(target = "dob", source = "profile.dob")
    UserViewProfileResponse toUserViewProfileResponse(User user);

    @Mapping(target = "avatarUrl", source = "profile.avatarUrl")
    UserSummary toUserSummary(User user);

    @Mapping(target = "displayName", source = "profile.displayName")
    @Mapping(target = "avatarUrl", source = "profile.avatarUrl")
    @Mapping(target = "bio", source = "profile.bio")
    @Mapping(target = "roles", expression = "java(mapRoleNames(user.getRoles()))")
    @Mapping(target = "isLocked", expression = "java(user.isLocked())")
    AdminUserResponse toAdminUserResponse(User user);

    default Set<String> mapRoleNames(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}
