package com.socialpulse.app.discovery.application.service;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.socialpulse.app.discovery.application.dto.response.TrendingHashtagResponse;
import com.socialpulse.app.discovery.application.usecase.GetTrendingHashtagsUseCase;
import com.socialpulse.app.feed.application.service.ContentAnalysisService;
import com.socialpulse.app.post.domain.repository.PostRepository;

@Service
public class GetTrendingHashtagsService implements GetTrendingHashtagsUseCase {
    private final PostRepository postRepository;
    private final ContentAnalysisService contentAnalysisService;

    public GetTrendingHashtagsService(
            PostRepository postRepository,
            ContentAnalysisService contentAnalysisService) {
        this.postRepository = postRepository;
        this.contentAnalysisService = contentAnalysisService;
    }

    @Override
    public List<TrendingHashtagResponse> getTrendingHashtags(int days, int limit) {
        int safeDays = Math.max(1, days);
        int safeLimit = Math.max(1, Math.min(limit, 100));

        Map<String, Long> counts = postRepository.findRecentPublicActiveSince(LocalDateTime.now().minusDays(safeDays))
                .stream()
                .flatMap(post -> contentAnalysisService.extractHashtags(post.getContent()).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(safeLimit)
                .map(entry -> TrendingHashtagResponse.builder()
                        .hashtag(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .toList();
    }
}
