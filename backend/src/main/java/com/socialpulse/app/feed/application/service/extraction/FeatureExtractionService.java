package com.socialpulse.app.feed.application.service.extraction;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.socialpulse.app.feed.application.dto.features.core.RankingFeatures;
import com.socialpulse.app.feed.application.usecase.extraction.ExtractFeaturesUseCase;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.feed.domain.model.UserInteractionAggregate;
import com.socialpulse.app.feed.domain.repository.UserInteractionRepository;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class FeatureExtractionService implements ExtractFeaturesUseCase {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final UserInteractionRepository userInteractionRepository;
    private final PostFeatureExtractor postFeatureExtractor;
    private final AuthorFeatureExtractor authorFeatureExtractor;
    private final InteractionFeatureExtractor interactionFeatureExtractor;

    public FeatureExtractionService(
            UserRepository userRepository,
            PostRepository postRepository,
            UserInteractionRepository userInteractionRepository,
            PostFeatureExtractor postFeatureExtractor,
            AuthorFeatureExtractor authorFeatureExtractor,
            InteractionFeatureExtractor interactionFeatureExtractor) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.userInteractionRepository = userInteractionRepository;
        this.postFeatureExtractor = postFeatureExtractor;
        this.authorFeatureExtractor = authorFeatureExtractor;
        this.interactionFeatureExtractor = interactionFeatureExtractor;
    }

    @Override
    public List<RankingFeatures> extractFeatures(Long viewerId, List<CandidatePost> candidates) {
        if (candidates.isEmpty()) return List.of();

        LocalDateTime now = LocalDateTime.now();

        Set<Long> authorIds = candidates.stream()
                .map(c -> c.getPost().getUserId())
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userRepository.findByIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Long> postCountMap = postRepository.countByUserIds(authorIds);
        Map<Long, Double> avgPopularityMap = postRepository.averagePopularityByUserIds(authorIds);
        long viewerTotal = viewerId != null
                ? userInteractionRepository.countTotalByViewerSince(viewerId, now.minusDays(30))
                : 0L;
        Map<Long, UserInteractionAggregate> aggregates = viewerId != null
                ? userInteractionRepository.findAggregatesByViewerAndAuthors(viewerId, authorIds, now.minusDays(30), now.minusDays(7))
                : Collections.emptyMap();

        return candidates.stream().map(candidate -> {
            Post post = candidate.getPost();
            UserInteractionAggregate agg = aggregates.get(post.getUserId());
            return RankingFeatures.builder()
                    .postId(post.getId())
                    .postFeatures(postFeatureExtractor.extract(post, now))
                    .authorFeatures(authorFeatureExtractor.extract(post.getUserId(), userMap, postCountMap, avgPopularityMap))
                    .interactionFeatures(interactionFeatureExtractor.extractFromAggregate(agg, now, viewerTotal))
                    .build();
        }).toList();
    }
}
