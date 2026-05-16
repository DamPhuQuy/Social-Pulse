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
    private final FeedItemResponseAssembler feedItemResponseAssembler;

    public GetFeedService(
            RankFeedUseCase rankFeedUseCase,
            FeedItemResponseAssembler feedItemResponseAssembler) {
        this.rankFeedUseCase = rankFeedUseCase;
        this.feedItemResponseAssembler = feedItemResponseAssembler;
    }

    @Override
    public List<FeedItemResponse> getFeed(int page, int size, CustomUserDetails currentUser) {
        Long userId = currentUser.getId();
        List<FeedItem> feedItems = rankFeedUseCase.getPaginatedFeed(userId, page, size);
        return feedItemResponseAssembler.assemble(feedItems, userId);
    }
}
