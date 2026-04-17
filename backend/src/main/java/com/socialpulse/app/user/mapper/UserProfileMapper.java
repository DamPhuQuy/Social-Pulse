package com.socialpulse.app.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.user.dto.response.UserViewProfileResponse;
import com.socialpulse.app.user.entity.User;
import com.socialpulse.app.user.entity.UserProfile;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @Mapping(target = "userId", source = "user.id")
    UserViewProfileResponse toUserViewProfileResponse(UserProfile userProfile);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "displayName", source = "profile.displayName")
    @Mapping(target = "bio", source = "profile.bio")
    @Mapping(target = "avatarUrl", source = "profile.avatarUrl")
    @Mapping(target = "dob", source = "profile.dob")
    UserViewProfileResponse toUserViewProfileResponse(User user);
}
