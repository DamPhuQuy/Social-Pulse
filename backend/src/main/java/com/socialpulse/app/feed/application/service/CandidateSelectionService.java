package com.socialpulse.app.feed.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.feed.domain.repository.FeedRepository;
import com.socialpulse.app.post.domain.model.Post;

@Service
public class CandidateSelectionService {
    private final FeedRepository feedRepository;

    private static final int RECENT_COUNT = 200;
    private static final int FOLLOWING_COUNT = 100;
    private static final int POPULAR_COUNT = 100;
    private static final int RANDOM_COUNT = 100;
    private static final int LOOKBACK_DAYS = 7;

    public CandidateSelectionService(FeedRepository feedRepository) {
        this.feedRepository = feedRepository;
    }

    public List<CandidatePost> selectCandidates(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(LOOKBACK_DAYS);
        List<CandidatePost> candidates = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        List<Post> recentPosts = feedRepository.findRecentPosts(since, PageRequest.of(0, RECENT_COUNT));
        for (Post post : recentPosts) {
            if (seenIds.add(post.getId())) {
                candidates.add(CandidatePost.builder()
                        .post(post)
                        .source("RECENT")
                        .build());
            }
        }

        List<Post> followingPosts = feedRepository.findFollowingPosts(userId, since, PageRequest.of(0, FOLLOWING_COUNT));
        for (Post post : followingPosts) {
            if (seenIds.add(post.getId())) {
                candidates.add(CandidatePost.builder()
                        .post(post)
                        .source("FOLLOWING")
                        .build());
            }
        }

        List<Post> popularPosts = feedRepository.findPopularPosts(since, PageRequest.of(0, POPULAR_COUNT));
        for (Post post : popularPosts) {
            if (seenIds.add(post.getId())) {
                candidates.add(CandidatePost.builder()
                        .post(post)
                        .source("POPULAR")
                        .build());
            }
        }

        List<Long> excludeIds = new ArrayList<>(seenIds);
        List<Post> randomPosts = feedRepository.findRandomPosts(excludeIds, PageRequest.of(0, RANDOM_COUNT));
        for (Post post : randomPosts) {
            if (seenIds.add(post.getId())) {
                candidates.add(CandidatePost.builder()
                        .post(post)
                        .source("RANDOM")
                        .build());
            }
        }

        return candidates;
    }
}
