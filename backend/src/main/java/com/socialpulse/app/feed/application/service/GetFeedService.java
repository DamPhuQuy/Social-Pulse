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
import com.socialpulse.app.security.user.CustomUserDetails;

@Service
public class GetFeedService implements GetFeedUseCase {
    private static final String SEEN_POSTS_PREFIX = "user:seen:";

    private final RankFeedUseCase rankFeedUseCase;
    private final FeedItemResponseAssembler feedItemResponseAssembler;
    private final CacheFeedUseCase cacheFeedUseCase;
    private final StringRedisTemplate redisTemplate;

    public GetFeedService(
            RankFeedUseCase rankFeedUseCase,
            FeedItemResponseAssembler feedItemResponseAssembler,
            CacheFeedUseCase cacheFeedUseCase,
            StringRedisTemplate redisTemplate) {
        this.rankFeedUseCase = rankFeedUseCase;
        this.feedItemResponseAssembler = feedItemResponseAssembler;
        this.cacheFeedUseCase = cacheFeedUseCase;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<FeedItemResponse> getFeed(int page, int size, CustomUserDetails currentUser) {
        Long userId = currentUser.getId();

        if (page == 0) {
            cacheFeedUseCase.invalidateFeed(userId);
            redisTemplate.delete(getSeenPostsKey(userId));
        }

        List<FeedItem> feedItems = rankFeedUseCase.getPaginatedFeed(userId, page, size);
        markSeen(userId, feedItems);

        return feedItemResponseAssembler.assemble(feedItems, userId);
    }

    @Override
    public List<FeedItemResponse> getFeed(int page, int size, String topicSlug, CustomUserDetails currentUser) {
        Long userId = currentUser.getId();

        if (page == 0) {
            cacheFeedUseCase.invalidateFeed(userId);
            redisTemplate.delete(getSeenPostsKey(userId));
        }

        List<FeedItem> feedItems = rankFeedUseCase.getPaginatedFeed(userId, page, size, topicSlug);
        markSeen(userId, feedItems);

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
