package com.socialpulse.app.admin.application.service;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

import com.socialpulse.app.admin.application.dto.SystemMetricsResponse;
import com.socialpulse.app.admin.application.usecase.GetSystemMetricsUseCase;
import com.socialpulse.app.feed.domain.repository.FeedImpressionRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class GetSystemMetricsService implements GetSystemMetricsUseCase {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FeedImpressionRepository feedImpressionRepository;

    public GetSystemMetricsService(
            UserRepository userRepository,
            PostRepository postRepository,
            FeedImpressionRepository feedImpressionRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.feedImpressionRepository = feedImpressionRepository;
    }

    @Override
    public SystemMetricsResponse getMetrics(String period) {
        LocalDateTime since = resolveSince(period);

        long totalUsers = userRepository.countAll();
        long newUsers = since != null ? userRepository.countByCreatedAtAfter(since) : totalUsers;
        Map<String, Long> usersByStatus = userRepository.countByStatus();

        long totalPosts = postRepository.countAll();
        long newPosts = since != null ? postRepository.countByCreatedAtAfter(since) : totalPosts;
        long toxicPosts = postRepository.countToxic();
        long deletedPosts = since != null ? postRepository.countDeletedAfter(since) : 0L;
        long totalFeedImpressions = feedImpressionRepository.countAll();
        long newFeedImpressions = feedImpressionRepository.countByCreatedAtAfter(since);
        long aiRankedImpressions = feedImpressionRepository.countByRankingProviderSince("AI", since);
        long fallbackRankedImpressions = feedImpressionRepository.countByRankingProviderSince("FALLBACK", since);

        return SystemMetricsResponse.builder()
                .generatedAt(LocalDateTime.now())
                .period(period)
                .totalUsers(totalUsers)
                .newUsers(newUsers)
                .usersByStatus(usersByStatus)
                .totalPosts(totalPosts)
                .newPosts(newPosts)
                .toxicPosts(toxicPosts)
                .deletedPosts(deletedPosts)
                .totalFeedImpressions(totalFeedImpressions)
                .newFeedImpressions(newFeedImpressions)
                .aiRankedImpressions(aiRankedImpressions)
                .fallbackRankedImpressions(fallbackRankedImpressions)
                .build();
    }

    @Override
    public byte[] exportCsv(String period) {
        SystemMetricsResponse m = getMetrics(period);
        StringBuilder sb = new StringBuilder("metric,value\n")
                .append("period,").append(m.getPeriod()).append("\n")
                .append("totalUsers,").append(m.getTotalUsers()).append("\n")
                .append("newUsers,").append(m.getNewUsers()).append("\n")
                .append("totalPosts,").append(m.getTotalPosts()).append("\n")
                .append("newPosts,").append(m.getNewPosts()).append("\n")
                .append("toxicPosts,").append(m.getToxicPosts()).append("\n")
                .append("deletedPosts,").append(m.getDeletedPosts()).append("\n")
                .append("totalFeedImpressions,").append(m.getTotalFeedImpressions()).append("\n")
                .append("newFeedImpressions,").append(m.getNewFeedImpressions()).append("\n")
                .append("aiRankedImpressions,").append(m.getAiRankedImpressions()).append("\n")
                .append("fallbackRankedImpressions,").append(m.getFallbackRankedImpressions()).append("\n");
        m.getUsersByStatus().forEach((status, count) ->
                sb.append("users_").append(status.toLowerCase()).append(",").append(count).append("\n"));
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private LocalDateTime resolveSince(String period) {
        return switch (period.toUpperCase()) {
            case "LAST_7_DAYS"  -> LocalDateTime.now().minusDays(7);
            case "LAST_30_DAYS" -> LocalDateTime.now().minusDays(30);
            case "LAST_90_DAYS" -> LocalDateTime.now().minusDays(90);
            default             -> null; // ALL_TIME
        };
    }
}
