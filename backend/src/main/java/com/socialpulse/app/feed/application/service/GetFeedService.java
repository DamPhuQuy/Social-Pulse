package com.socialpulse.app.feed.application.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.socialpulse.app.feed.application.dto.response.FeedItemResponse;
import com.socialpulse.app.feed.application.usecase.GetFeedUseCase;
import com.socialpulse.app.feed.domain.model.FeedItem;
import com.socialpulse.app.security.user.CustomUserDetails;

@Service
public class GetFeedService implements GetFeedUseCase {
    private final FeedRankingService feedRankingService;

    public GetFeedService(FeedRankingService feedRankingService) {
        this.feedRankingService = feedRankingService;
    }

    @Override
    public List<FeedItemResponse> getFeed(int page, int size, CustomUserDetails currentUser) {
        Long userId = currentUser.getUserId();

        List<FeedItem> feedItems = feedRankingService.getPaginatedFeed(userId, page, size);

        return feedItems.stream()
                .map(item -> FeedItemResponse.builder()
                        .postId(item.getPostId())
                        .aiScore(item.getAiScore())
                        .source(item.getSource())
                        .rankedAt(item.getRankedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
