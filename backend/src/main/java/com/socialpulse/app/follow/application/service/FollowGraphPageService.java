package com.socialpulse.app.follow.application.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.user.application.dto.mapper.UserMapper;
import com.socialpulse.app.user.application.dto.response.UserSummary;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class FollowGraphPageService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public FollowGraphPageService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public PageResponse<UserSummary> build(Page<Long> userIdPage) {
        List<Long> orderedIds = userIdPage.getContent();
        Map<Long, User> userMap = userRepository.findAllById(orderedIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<UserSummary> items = orderedIds.stream()
                .map(userMap::get)
                .filter(user -> user != null)
                .map(userMapper::toUserSummary)
                .toList();

        return PageResponse.<UserSummary>builder()
                .items(items)
                .page(userIdPage.getNumber())
                .size(userIdPage.getSize())
                .totalElements(userIdPage.getTotalElements())
                .totalPages(userIdPage.getTotalPages())
                .hasNext(userIdPage.hasNext())
                .build();
    }
}
