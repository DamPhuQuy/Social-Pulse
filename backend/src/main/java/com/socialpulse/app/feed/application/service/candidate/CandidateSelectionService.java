package com.socialpulse.app.feed.application.service.candidate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.socialpulse.app.feed.application.usecase.candidate.SelectCandidatesUseCase;
import com.socialpulse.app.feed.domain.enums.Source;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.feed.domain.repository.FeedRepository;
import com.socialpulse.app.post.domain.model.Post;

public class CandidateSelectionService implements SelectCandidatesUseCase {
    private final FeedRepository feedRepository;
    private final StringRedisTemplate redisTemplate;

    private static final int RECENT_COUNT = 200;
    private static final int FOLLOWING_COUNT = 100;
    private static final int POPULAR_COUNT = 100;
    private static final int RANDOM_COUNT = 100;
    private static final int LOOKBACK_DAYS = 7;
    private static final int EXTENDED_LOOKBACK_DAYS = 30;
    private static final int MIN_CANDIDATES = 20;

    public CandidateSelectionService(FeedRepository feedRepository, StringRedisTemplate redisTemplate) {
        this.feedRepository = feedRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<CandidatePost> selectCandidates(Long userId) {
        List<CandidatePost> candidates = collectCandidates(userId, LocalDateTime.now().minusDays(LOOKBACK_DAYS));

        // Extend lookback window if not enough candidates
        if (candidates.size() < MIN_CANDIDATES) {
            candidates = collectCandidates(userId, LocalDateTime.now().minusDays(EXTENDED_LOOKBACK_DAYS));
        }

        return candidates;
    }

    private List<CandidatePost> collectCandidates(Long userId, LocalDateTime since) {
        List<CandidatePost> candidates = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        // Initialize seenIds with the user's view history to filter out seen posts
        String seenKey = "user:seen:" + userId;
        Set<String> history = redisTemplate.opsForSet().members(seenKey);
        if (history != null) {
            for (String idStr : history) {
                try {
                    seenIds.add(Long.parseLong(idStr));
                } catch (NumberFormatException ignored) {}
            }
        }

        List<Post> recentPosts = feedRepository.findRecentPosts(since, PageRequest.of(0, RECENT_COUNT));
        for (Post post : recentPosts) {
            if (seenIds.add(post.getId())) {
                candidates.add(CandidatePost.builder()
                        .post(post)
                        .source(Source.RECENT)
                        .build());
            }
        }

        List<Post> followingPosts = feedRepository.findFollowingPosts(userId, since, PageRequest.of(0, FOLLOWING_COUNT));
        for (Post post : followingPosts) {
            if (seenIds.add(post.getId())) {
                candidates.add(CandidatePost.builder()
                        .post(post)
                        .source(Source.FOLLOWING)
                        .build());
            }
        }

        List<Post> popularPosts = feedRepository.findPopularPosts(since, PageRequest.of(0, POPULAR_COUNT));
        for (Post post : popularPosts) {
            if (seenIds.add(post.getId())) {
                candidates.add(CandidatePost.builder()
                        .post(post)
                        .source(Source.POPULAR)
                        .build());
            }
        }

        List<Long> excludeIds = new ArrayList<>(seenIds);
        List<Post> randomPosts = feedRepository.findRandomPosts(excludeIds, PageRequest.of(0, RANDOM_COUNT));
        for (Post post : randomPosts) {
            if (seenIds.add(post.getId())) {
                candidates.add(CandidatePost.builder()
                        .post(post)
                        .source(Source.RANDOM)
                        .build());
            }
        }

        return candidates;
    }
}
