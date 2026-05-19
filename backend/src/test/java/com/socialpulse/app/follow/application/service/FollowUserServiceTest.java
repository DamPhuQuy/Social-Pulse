package com.socialpulse.app.follow.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.follow.application.dto.mapper.FollowMapper;
import com.socialpulse.app.follow.application.dto.response.FollowResponse;
import com.socialpulse.app.follow.domain.model.Follow;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.notification.application.service.NotificationCommandService;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FollowUserServiceTest {

    @Mock private FollowRepository followRepository;
    @Mock private UserRepository userRepository;
    @Mock private FollowMapper followMapper;
    @Mock private NotificationCommandService notificationCommandService;

    private FollowUserService service;

    @BeforeEach
    void setUp() {
        service = new FollowUserService(followRepository, userRepository, followMapper, notificationCommandService);
    }

    private CustomUserDetails userDetails(Long id) {
        User user = User.builder().id(id).email(id + "@mail.com").username("user" + id).roles(Set.of()).build();
        return new CustomUserDetails(user);
    }

    @Test
    void followUser_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).build()));
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);
        when(followRepository.save(any())).thenReturn(Follow.builder().id(1L).followerId(1L).followingId(2L).build());
        when(followMapper.toFollowResponse(any())).thenReturn(FollowResponse.builder().build());

        FollowResponse result = service.followUser(2L, userDetails(1L));

        assertNotNull(result);
        verify(notificationCommandService).notifyFollow(1L, 2L);
    }

    @Test
    void followUser_selfFollow_throws() {
        assertThrows(AppException.class, () -> service.followUser(1L, userDetails(1L)));
    }

    @Test
    void followUser_alreadyFollowing_throws() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).build()));
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

        assertThrows(AppException.class, () -> service.followUser(2L, userDetails(1L)));
    }

    @Test
    void followUser_targetNotFound_throws() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> service.followUser(2L, userDetails(1L)));
    }
}
