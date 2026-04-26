package com.socialpulse.app.follow.application.dto.mapper;

import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import com.socialpulse.app.follow.application.dto.response.FollowResponse;
import com.socialpulse.app.follow.domain.model.Follow;

@Mapper(componentModel = "spring")
public interface FollowMapper {
    FollowResponse toFollowResponse(Follow follow);
}
