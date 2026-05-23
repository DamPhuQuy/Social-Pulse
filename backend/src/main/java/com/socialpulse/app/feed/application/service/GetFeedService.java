package com.socialpulse.app.feed.application.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.socialpulse.app.feed.application.dto.response.FeedItemResponse;
import com.socialpulse.app.feed.application.service.assembler.FeedItemResponseAssembler;
import com.socialpulse.app.feed.application.usecase.GetFeedUseCase;
import com.socialpulse.app.feed.application.usecase.cache.CacheFeedUseCase;
import com.socialpulse.app.feed.application.usecase.ranking.RankFeedUseCase;
import com.socialpulse.app.feed.domain.model.FeedItem;
import com.socialpulse.app.feed.domain.repository.FeedImpressionRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

@Service
public class GetFeedService implements GetFeedUseCase {
    private static final String SEEN_POSTS_PREFIX = "user:seen:";

    private final RankFeedUseCase rankFeedUseCase;
    private final FeedItemResponseAssembler feedItemResponseAssembler;
    private final CacheFeedUseCase cacheFeedUseCase;
    private final StringRedisTemplate redisTemplate;
    private final FeedImpressionRepository feedImpressionRepository;

    public GetFeedService(
            RankFeedUseCase rankFeedUseCase,
            FeedItemResponseAssembler feedItemResponseAssembler,
            CacheFeedUseCase cacheFeedUseCase,
            StringRedisTemplate redisTemplate,
            FeedImpressionRepository feedImpressionRepository) {
        this.rankFeedUseCase = rankFeedUseCase;
        this.feedItemResponseAssembler = feedItemResponseAssembler;
        this.cacheFeedUseCase = cacheFeedUseCase;
        this.redisTemplate = redisTemplate;
        this.feedImpressionRepository = feedImpressionRepository;
    }

    @Override
    public List<FeedItemResponse> getFeed(int page, int size, CustomUserDetails currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : null;

        if (page == 0 && userId != null) {
            cacheFeedUseCase.invalidateFeed(userId);
        }

        List<FeedItem> feedItems = rankFeedUseCase.getPaginatedFeed(userId, page, size);
        if (userId != null) {
            markSeen(userId, feedItems);
            feedImpressionRepository.saveAll(userId, feedItems, page, size, "HOME");
        }

        return feedItemResponseAssembler.assemble(feedItems, userId);
    }

    @Override
    public List<FeedItemResponse> getFeed(int page, int size, String topicSlug, CustomUserDetails currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : null;

        if (page == 0 && userId != null) {
            cacheFeedUseCase.invalidateFeed(userId);
        }

        List<FeedItem> feedItems = rankFeedUseCase.getPaginatedFeed(userId, page, size, topicSlug);
        if (userId != null) {
            markSeen(userId, feedItems);
            feedImpressionRepository.saveAll(userId, feedItems, page, size, "TOPIC:" + topicSlug);
        }

        return feedItemResponseAssembler.assemble(feedItems, userId);
    }

    private void markSeen(Long userId, List<FeedItem> feedItems) {
        if (feedItems.isEmpty()) return;
        String[] postIds = feedItems.stream()
            .map(item -> String.valueOf(item.getPostId()))
            .toArray(String[]::new);
        String seenKey = getSeenPostsKey(userId);
        redisTemplate.opsForSet().add(seenKey, postIds);
        redisTemplate.expire(seenKey, Duration.ofDays(7));
    }

    private String getSeenPostsKey(Long userId) {
        return SEEN_POSTS_PREFIX + userId;
    }
}
