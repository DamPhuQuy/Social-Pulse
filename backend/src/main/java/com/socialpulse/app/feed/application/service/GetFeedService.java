package com.socialpulse.app.feed.application.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.socialpulse.app.feed.application.dto.response.FeedItemResponse;
import com.socialpulse.app.feed.application.usecase.CacheFeedUseCase;
import com.socialpulse.app.feed.application.usecase.GetFeedUseCase;
import com.socialpulse.app.feed.application.usecase.RankFeedUseCase;
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
    public List<FeedItemResponse> getFeed(int page, int size, CustomUserDetails currentUser, String topicSlug) {
        Long userId = currentUser.getId();

        // A full refresh should rebuild the feed from scratch instead of permanently
        // exhausting it with previous seen-history entries.
        if (page == 0) {
            cacheFeedUseCase.invalidateFeed(userId);
            redisTemplate.delete(getSeenPostsKey(userId));
        }

        List<FeedItem> feedItems = rankFeedUseCase.getPaginatedFeed(userId, page, size);

        // Keep seen history within the current feed session so pagination avoids duplicates.
        if (!feedItems.isEmpty()) {
            String[] postIds = feedItems.stream()
                .map(item -> String.valueOf(item.getPostId()))
                .toArray(String[]::new);
            String seenKey = getSeenPostsKey(userId);
            redisTemplate.opsForSet().add(seenKey, postIds);
            redisTemplate.expire(seenKey, Duration.ofDays(7)); // Keep seen history for 7 days
        }

        List<FeedItemResponse> assembled = feedItemResponseAssembler.assemble(feedItems, userId);

        // If a topicSlug filter is requested, filter the assembled responses by that topic.
        if (topicSlug != null && !topicSlug.isBlank()) {
            assembled = assembled.stream()
                .filter(item -> item.getTopicSlugs() != null && item.getTopicSlugs().contains(topicSlug))
                .toList();
        }

        return assembled;
    }

    private String getSeenPostsKey(Long userId) {
        return SEEN_POSTS_PREFIX + userId;
    }
}
