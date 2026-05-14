package com.socialpulse.app.feed.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.socialpulse.app.feed.application.dto.response.FeedItemResponse;
import com.socialpulse.app.feed.application.usecase.GetFeedUseCase;
import com.socialpulse.app.feed.application.usecase.RankFeedUseCase;
import com.socialpulse.app.feed.domain.model.FeedItem;
import com.socialpulse.app.security.user.CustomUserDetails;

@Service
public class GetFeedService implements GetFeedUseCase {
    private final RankFeedUseCase rankFeedUseCase;

    public GetFeedService(RankFeedUseCase rankFeedUseCase) {
        this.rankFeedUseCase = rankFeedUseCase;
    }

    @Override
    public List<FeedItemResponse> getFeed(int page, int size, CustomUserDetails currentUser) {
        Long userId = currentUser.getId();
        List<FeedItem> feedItems = rankFeedUseCase.getPaginatedFeed(userId, page, size);

        return feedItems.stream()
                .map(item -> FeedItemResponse.builder()
                        .postId(item.getPostId())
                        .aiScore(item.getAiScore())
                        .source(item.getSource().name())
                        .rankedAt(item.getRankedAt())
                        .build())
                .toList();
    }
}
