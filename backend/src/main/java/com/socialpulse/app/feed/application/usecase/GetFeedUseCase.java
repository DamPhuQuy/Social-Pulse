package com.socialpulse.app.feed.application.usecase;

import java.util.List;

import com.socialpulse.app.feed.application.dto.response.FeedItemResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface GetFeedUseCase {
    List<FeedItemResponse> getFeed(int page, int size, CustomUserDetails currentUser);
}
