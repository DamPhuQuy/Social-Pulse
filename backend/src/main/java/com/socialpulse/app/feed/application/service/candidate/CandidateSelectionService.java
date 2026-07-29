package com.socialpulse.app.feed.application.service.candidate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.socialpulse.app.block.domain.model.Block;
import com.socialpulse.app.block.domain.repository.BlockRepository;
import com.socialpulse.app.feed.application.usecase.candidate.SelectCandidatesUseCase;
import com.socialpulse.app.feed.domain.enums.Source;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.feed.domain.repository.FeedRepository;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.topic.infrastructure.persistence.repository.JpaTopicFollowRepository;

@Service
public class CandidateSelectionService implements SelectCandidatesUseCase {
    private final FeedRepository feedRepository;
    private final StringRedisTemplate redisTemplate;
    private final BlockRepository blockRepository;
    private final JpaTopicFollowRepository topicFollowRepository;

    private static final int RECENT_COUNT = 150;
    private static final int FOLLOWING_COUNT = 100;
    private static final int POPULAR_COUNT = 100;
    private static final int LOOKBACK_DAYS = 14;
    private static final int EXTENDED_LOOKBACK_DAYS = 60;
    private static final int MIN_CANDIDATES = 15;

    public CandidateSelectionService(FeedRepository feedRepository, 
                                     StringRedisTemplate redisTemplate,
                                     BlockRepository blockRepository,
                                     JpaTopicFollowRepository topicFollowRepository) {
        this.feedRepository = feedRepository;
        this.redisTemplate = redisTemplate;
        this.blockRepository = blockRepository;
        this.topicFollowRepository = topicFollowRepository;
    }

    @Override
    public List<CandidatePost> selectCandidates(Long userId) {
        List<CandidatePost> candidates = collectCandidates(userId, LocalDateTime.now().minusDays(LOOKBACK_DAYS));
        if (candidates.size() < MIN_CANDIDATES) {
            candidates = collectCandidates(userId, LocalDateTime.now().minusDays(EXTENDED_LOOKBACK_DAYS));
        }
        return candidates;
    }

    @Override
    public List<CandidatePost> selectCandidatesByTopic(String topicSlug) {
        List<Post> posts = feedRepository.findByTopicSlug(topicSlug,
                LocalDateTime.now().minusDays(EXTENDED_LOOKBACK_DAYS),
                PageRequest.of(0, RECENT_COUNT + POPULAR_COUNT));
        return posts.stream()
                .map(post -> CandidatePost.builder().post(post).source(Source.TOPIC).build())
                .toList();
    }

    private List<CandidatePost> collectCandidates(Long userId, LocalDateTime since) {
        List<CandidatePost> candidates = new ArrayList<>();
        Set<Long> addedPostIds = new HashSet<>();

        // Fetch user blocking graph to filter out content
        Set<Long> blockedUserIds = new HashSet<>();
        if (userId != null) {
            blockedUserIds.addAll(
                blockRepository.findByBlockerId(userId).stream()
                    .map(Block::getBlockedId)
                    .collect(Collectors.toSet())
            );
            blockedUserIds.addAll(
                blockRepository.findByBlockedId(userId).stream()
                    .map(Block::getBlockerId)
                    .collect(Collectors.toSet())
            );
        }

        // 1. Fetch Following User & Topic Posts
        if (userId != null) {
            List<Post> followingPosts = feedRepository.findFollowingUserAndTopicPosts(
                    userId, since, PageRequest.of(0, FOLLOWING_COUNT));

            for (Post post : followingPosts) {
                if (blockedUserIds.contains(post.getUserId())) continue;
                if (addedPostIds.add(post.getId())) {
                    candidates.add(CandidatePost.builder().post(post).source(Source.FOLLOWING).build());
                }
            }
        }

        // 2. Fetch Popular / Engaging Posts (Discover Feed)
        List<Post> popularPosts = feedRepository.findPopularPosts(since, PageRequest.of(0, POPULAR_COUNT));
        for (Post post : popularPosts) {
            if (blockedUserIds.contains(post.getUserId())) continue;
            if (addedPostIds.add(post.getId())) {
                candidates.add(CandidatePost.builder().post(post).source(Source.POPULAR).build());
            }
        }

        // 3. Fetch Recent Posts (Fallback & Fresh Content)
        List<Post> recentPosts = feedRepository.findRecentPosts(since, PageRequest.of(0, RECENT_COUNT));
        for (Post post : recentPosts) {
            if (blockedUserIds.contains(post.getUserId())) continue;
            if (addedPostIds.add(post.getId())) {
                candidates.add(CandidatePost.builder().post(post).source(Source.RECENT).build());
            }
        }

        return candidates;
    }
}
