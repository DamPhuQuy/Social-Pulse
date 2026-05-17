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
        
        // Pull-to-refresh (page 0): invalidate the cache to ensure fresh posts are fetched,
        // and seen posts are filtered out by CandidateSelectionService.
        if (page == 0) {
            cacheFeedUseCase.invalidateFeed(userId);
        }

        List<FeedItem> feedItems = rankFeedUseCase.getPaginatedFeed(userId, page, size);
        
        // Mark these items as seen so they won't appear in future feed generations
        if (!feedItems.isEmpty()) {
            String[] postIds = feedItems.stream()
                .map(item -> String.valueOf(item.getPostId()))
                .toArray(String[]::new);
            String seenKey = "user:seen:" + userId;
            redisTemplate.opsForSet().add(seenKey, postIds);
            redisTemplate.expire(seenKey, Duration.ofDays(7)); // Keep seen history for 7 days
        }

        return feedItemResponseAssembler.assemble(feedItems, userId);
    }
}
