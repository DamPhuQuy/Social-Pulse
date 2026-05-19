package com.socialpulse.app.follow.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.model.User;

@ExtendWith(MockitoExtension.class)
class UnfollowUserServiceTest {

    @Mock private FollowRepository followRepository;

    private UnfollowUserService service;

    @BeforeEach
    void setUp() {
        service = new UnfollowUserService(followRepository);
    }

    private CustomUserDetails userDetails(Long id) {
        User user = User.builder().id(id).email(id + "@mail.com").username("user" + id).roles(Set.of()).build();
        return new CustomUserDetails(user);
    }

    @Test
    void unfollowUser_success() {
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

        service.unfollowUser(2L, userDetails(1L));

        verify(followRepository).deleteByFollowerIdAndFollowingId(1L, 2L);
    }

    @Test
    void unfollowUser_notFollowing_throws() {
        when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);

        assertThrows(AppException.class, () -> service.unfollowUser(2L, userDetails(1L)));
    }
}
