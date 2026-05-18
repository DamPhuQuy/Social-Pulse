package com.socialpulse.app.admin.application.service;

import java.time.LocalDateTime;
import java.util.Map;

import com.socialpulse.app.admin.application.dto.SystemMetricsResponse;
import com.socialpulse.app.admin.application.usecase.GetSystemMetricsUseCase;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class GetSystemMetricsService implements GetSystemMetricsUseCase {
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public GetSystemMetricsService(UserRepository userRepository, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
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
                .append("deletedPosts,").append(m.getDeletedPosts()).append("\n");
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
