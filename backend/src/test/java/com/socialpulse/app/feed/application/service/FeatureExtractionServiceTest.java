package com.socialpulse.app.feed.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.feed.domain.enums.Source;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.enums.UserStatus;
import com.socialpulse.app.user.domain.enums.VerificationStatus;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FeatureExtractionServiceTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Test
    void extractsPushshiftAlignedPostAndAuthorFeatures() {
        FeatureExtractionService service = new FeatureExtractionService(
                redisTemplate,
                new ObjectMapper(),
                userRepository,
                postRepository);

        CandidatePost candidate = CandidatePost.builder()
                .post(Post.builder()
                        .id(100L)
                        .userId(7L)
                        .content("pushshift aligned body")
                        .imageUrl("https://cdn.example.com/image.jpg")
                        .upvoteCount(20L)
                        .downvoteCount(3L)
                        .cmtCount(5L)
                        .shareCount(2L)
                        .viewCount(999L)
                        .hotScore(12.5)
                        .createdAt(LocalDateTime.now().minusHours(4))
                        .build())
                .source(Source.POPULAR)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("author:features:7")).thenReturn(null);
        when(userRepository.findByIds(Set.of(7L))).thenReturn(List.of(User.builder()
                .id(7L)
                .status(UserStatus.ACTIVE)
                .verification(VerificationStatus.VERIFIED)
                .createdAt(LocalDateTime.now().minusDays(730))
                .build()));
        when(postRepository.countByUserIds(Set.of(7L))).thenReturn(Map.of(7L, 11L));
        when(postRepository.averagePopularityByUserIds(Set.of(7L))).thenReturn(Map.of(7L, 18.5));

        var features = service.extractFeatures(42L, List.of(candidate));

        assertEquals(1, features.size());
        assertEquals(100L, features.get(0).getPostId());
        assertEquals(27.0, features.get(0).getPostFeatures().getPopularity(), 1e-9);
        assertEquals(18.5, features.get(0).getAuthorFeatures().getAveragePopularity(), 1e-9);
        assertEquals(11L, features.get(0).getAuthorFeatures().getPostCount());
        verify(postRepository).averagePopularityByUserIds(Set.of(7L));
    }
}
