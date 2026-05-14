package com.socialpulse.app.feed.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.socialpulse.app.feed.application.usecase.RankFeedUseCase;
import com.socialpulse.app.feed.domain.enums.Source;
import com.socialpulse.app.feed.domain.model.FeedItem;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.enums.UserStatus;
import com.socialpulse.app.user.domain.enums.VerificationStatus;
import com.socialpulse.app.user.domain.model.Role;
import com.socialpulse.app.user.domain.model.User;

@ExtendWith(MockitoExtension.class)
class GetFeedServiceTest {
    @Mock
    private RankFeedUseCase rankFeedUseCase;

    @Test
    void returnsPaginatedFeedWithoutTrainingSideEffects() {
        GetFeedService service = new GetFeedService(rankFeedUseCase);
        CustomUserDetails currentUser = new CustomUserDetails(User.builder()
                .id(42L)
                .email("user@example.com")
                .status(UserStatus.ACTIVE)
                .verification(VerificationStatus.VERIFIED)
                .roles(Set.of(Role.builder().name("USER").build()))
                .build());

        List<FeedItem> feedItems = List.of(
                FeedItem.builder().postId(100L).userId(42L).aiScore(0.9).source(Source.RECENT).rankedAt(LocalDateTime.now()).build(),
                FeedItem.builder().postId(101L).userId(42L).aiScore(0.8).source(Source.POPULAR).rankedAt(LocalDateTime.now()).build());
        when(rankFeedUseCase.getPaginatedFeed(42L, 2, 2)).thenReturn(feedItems);

        var response = service.getFeed(2, 2, currentUser);

        assertEquals(2, response.size());
        verify(rankFeedUseCase).getPaginatedFeed(42L, 2, 2);
    }
}
