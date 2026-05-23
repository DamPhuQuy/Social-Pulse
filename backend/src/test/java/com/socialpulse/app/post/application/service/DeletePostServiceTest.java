package com.socialpulse.app.post.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.realtime.application.service.SseEmitterRegistry;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.model.Permission;
import com.socialpulse.app.user.domain.model.Role;
import com.socialpulse.app.user.domain.model.User;

@ExtendWith(MockitoExtension.class)
class DeletePostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private SseEmitterRegistry sseEmitterRegistry;

    private DeletePostService service;

    @BeforeEach
    void setUp() {
        service = new DeletePostService(postRepository, redisTemplate, sseEmitterRegistry);
    }

    private CustomUserDetails userDetails(Long id) {
        User user = User.builder().id(id).email(id + "@mail.com").username("user" + id).roles(Set.of()).build();
        return new CustomUserDetails(user);
    }

    private CustomUserDetails adminDetails() {
        Permission perm = Permission.builder().name("post:manage").build();
        Role role = Role.builder().name("ADMIN").permissions(Set.of(perm)).build();
        User user = User.builder().id(99L).email("admin@mail.com").username("admin").roles(Set.of(role)).build();
        return new CustomUserDetails(user);
    }

    @Test
    void deletePost_asOwner_succeeds() {
        Post post = Post.builder().id(1L).userId(10L).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        service.deletePost(1L, userDetails(10L));

        verify(postRepository).deleteById(1L);
        verify(redisTemplate).delete("user:feed:10");
    }

    @Test
    void deletePost_asAdmin_succeeds() {
        Post post = Post.builder().id(1L).userId(10L).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        service.deletePost(1L, adminDetails());

        verify(postRepository).deleteById(1L);
        verify(redisTemplate).delete("user:feed:99");
    }

    @Test
    void deletePost_notOwnerNoPermission_throws() {
        Post post = Post.builder().id(1L).userId(10L).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThrows(AppException.class, () -> service.deletePost(1L, userDetails(99L)));
    }

    @Test
    void deletePost_notFound_throws() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> service.deletePost(1L, userDetails(10L)));
    }
}
