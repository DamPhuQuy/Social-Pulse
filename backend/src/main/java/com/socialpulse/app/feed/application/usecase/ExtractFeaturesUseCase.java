package com.socialpulse.app.feed.application.usecase;

import java.util.List;

import com.socialpulse.app.feed.application.dto.RankingFeatures;
import com.socialpulse.app.feed.domain.model.CandidatePost;

public interface ExtractFeaturesUseCase {
    List<RankingFeatures> extractFeatures(Long viewerId, List<CandidatePost> candidates);
}
