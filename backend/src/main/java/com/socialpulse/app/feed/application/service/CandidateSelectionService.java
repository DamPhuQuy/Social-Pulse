package com.socialpulse.app.feed.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.socialpulse.app.block.JpaBlockRepository;
import com.socialpulse.app.feed.application.usecase.SelectCandidatesUseCase;
import com.socialpulse.app.feed.domain.enums.Source;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.feed.domain.repository.FeedRepository;
import com.socialpulse.app.post.domain.model.Post;

public class CandidateSelectionService implements SelectCandidatesUseCase {
    private final FeedRepository feedRepository;
    private final StringRedisTemplate redisTemplate;
    private final JpaBlockRepository blockRepository;

    private static final int RECENT_COUNT = 200;
    private static final int FOLLOWING_COUNT = 100;
    private static final int POPULAR_COUNT = 100;
    private static final int RANDOM_COUNT = 100;
    private static final int LOOKBACK_DAYS = 7;

    public CandidateSelectionService(FeedRepository feedRepository, 
                                     StringRedisTemplate redisTemplate,
                                     JpaBlockRepository blockRepository) {
        this.feedRepository = feedRepository;
        this.redisTemplate = redisTemplate;
        this.blockRepository = blockRepository;
    }

    @Override
    public List<CandidatePost> selectCandidates(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(LOOKBACK_DAYS);
        List<CandidatePost> candidates = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        // Fetch user blocking graph to filter out content
        Set<Long> blockedUserIds = new HashSet<>();
        if (userId != null) {
            blockedUserIds.addAll(
                blockRepository.findByBlockerId(userId).stream()
                    .map(b -> b.getBlocked().getId())
                    .collect(Collectors.toSet())
            );
            blockedUserIds.addAll(
                blockRepository.findByBlockedId(userId).stream()
                    .map(b -> b.getBlocker().getId())
                    .collect(Collectors.toSet())
            );
        }

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
            if (blockedUserIds.contains(post.getUserId())) {
                continue;
            }
            if (seenIds.add(post.getId())) {
                candidates.add(CandidatePost.builder()
                        .post(post)
                        .source(Source.RECENT)
                        .build());
            }
        }

        List<Post> followingPosts = feedRepository.findFollowingPosts(userId, since, PageRequest.of(0, FOLLOWING_COUNT));
        for (Post post : followingPosts) {
            if (blockedUserIds.contains(post.getUserId())) {
                continue;
            }
            if (seenIds.add(post.getId())) {
                candidates.add(CandidatePost.builder()
                        .post(post)
                        .source(Source.FOLLOWING)
                        .build());
            }
        }

        List<Post> popularPosts = feedRepository.findPopularPosts(since, PageRequest.of(0, POPULAR_COUNT));
        for (Post post : popularPosts) {
            if (blockedUserIds.contains(post.getUserId())) {
                continue;
            }
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
            if (blockedUserIds.contains(post.getUserId())) {
                continue;
            }
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
