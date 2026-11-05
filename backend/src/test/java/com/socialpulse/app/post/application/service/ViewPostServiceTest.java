package com.socialpulse.app.post.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HyperLogLogOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.post.application.dto.mapper.PostMapper;
import com.socialpulse.app.post.application.dto.response.ViewPostResponse;
import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.model.PostReactions;
import com.socialpulse.app.post.domain.repository.PostReactionsRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ViewPostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private PostReactionsRepository postReactionsRepository;
    @Mock private UserRepository userRepository;
    @Mock private PostMapper postMapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private HyperLogLogOperations<String, String> hllOperations;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private SetOperations<String, String> setOperations;

    private ViewPostService viewPostService;

    @BeforeEach
    void setUp() {
        viewPostService = new ViewPostService(
                postRepository,
                postReactionsRepository,
                userRepository,
                postMapper,
                redisTemplate
        );
    }

    private CustomUserDetails userDetails(Long id) {
        User user = User.builder().id(id).email("u" + id + "@devpulse.tech").username("user" + id).roles(Set.of()).build();
        return new CustomUserDetails(user);
    }

    @Test
    @DisplayName("First-time viewer adds to Redis HyperLogLog and increments write-behind buffer without direct DB write")
    void viewPost_firstTime_buffersInRedis_noDirectDbWrite() {
        Long postId = 100L;
        Long viewerId = 42L;
        Long authorId = 1L;

        Post post = Post.builder()
                .id(postId)
                .userId(authorId)
                .privacy(Privacy.PUBLIC)
                .type(PostType.ORIGINAL)
                .viewCount(50L)
                .build();

        User author = User.builder().id(authorId).username("linus").roles(Set.of()).build();
        ViewPostResponse expectedResponse = ViewPostResponse.builder().id(postId).viewCount(50).build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(postReactionsRepository.findByPostIdAndUserId(postId, viewerId)).thenReturn(Optional.empty());
        when(postMapper.toViewPostResponse(eq(post), eq(author), eq(0))).thenReturn(expectedResponse);

        ViewPostResponse response = viewPostService.viewPost(postId, userDetails(viewerId));

        assertNotNull(response);
        assertEquals(50, response.getViewCount());

        // Verify direct atomic increment in repository
        verify(postRepository).incrementViewCount(postId);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("Viewing a post directly increments view count in repository")
    void viewPost_incrementsViewCount_directlyInRepository() {
        Long postId = 100L;
        Long viewerId = 42L;
        Long authorId = 1L;

        Post post = Post.builder()
                .id(postId)
                .userId(authorId)
                .privacy(Privacy.PUBLIC)
                .type(PostType.ORIGINAL)
                .viewCount(50L)
                .build();

        User author = User.builder().id(authorId).username("linus").roles(Set.of()).build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(postReactionsRepository.findByPostIdAndUserId(postId, viewerId)).thenReturn(Optional.empty());
        when(postMapper.toViewPostResponse(any(), any(), any())).thenReturn(ViewPostResponse.builder().id(postId).build());

        viewPostService.viewPost(postId, userDetails(viewerId));

        // Verify repository atomic increment
        verify(postRepository).incrementViewCount(postId);
    }

    @Test
    @DisplayName("Viewing a private post as non-author throws POST_NOT_ACCESSIBLE")
    void viewPost_privatePost_nonAuthor_throwsException() {
        Long postId = 100L;
        Post privatePost = Post.builder()
                .id(postId)
                .userId(1L)
                .privacy(Privacy.PRIVATE)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(privatePost));

        assertThrows(AppException.class, () -> viewPostService.viewPost(postId, userDetails(42L)));
        verify(postRepository, never()).save(any());
    }
}
