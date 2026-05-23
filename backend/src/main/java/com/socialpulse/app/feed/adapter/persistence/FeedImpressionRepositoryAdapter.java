package com.socialpulse.app.feed.adapter.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import com.socialpulse.app.feed.domain.model.FeedItem;
import com.socialpulse.app.feed.domain.repository.FeedImpressionRepository;

public class FeedImpressionRepositoryAdapter implements FeedImpressionRepository {
    private final JdbcTemplate jdbcTemplate;

    public FeedImpressionRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveAll(Long viewerId, List<FeedItem> feedItems, int page, int size, String feedContext) {
        if (viewerId == null || feedItems == null || feedItems.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO feed_impressions (
                    viewer_id,
                    post_id,
                    rank_position,
                    page_number,
                    page_size,
                    ai_score,
                    candidate_source,
                    ranking_provider,
                    feature_schema_version,
                    feed_context
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        int startRank = Math.max(page, 0) * Math.max(size, 0);
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                FeedItem item = feedItems.get(i);
                ps.setLong(1, viewerId);
                ps.setLong(2, item.getPostId());
                ps.setInt(3, startRank + i);
                ps.setInt(4, page);
                ps.setInt(5, size);
                if (item.getAiScore() != null) {
                    ps.setDouble(6, item.getAiScore());
                } else {
                    ps.setNull(6, java.sql.Types.DOUBLE);
                }
                ps.setString(7, item.getSource() != null ? item.getSource().name() : null);
                ps.setString(8, item.getRankingProvider() != null ? item.getRankingProvider().name() : "FALLBACK");
                ps.setString(9, item.getFeatureSchemaVersion());
                ps.setString(10, feedContext);
            }

            @Override
            public int getBatchSize() {
                return feedItems.size();
            }
        });
    }

    @Override
    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM feed_impressions", Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long countByCreatedAtAfter(LocalDateTime since) {
        if (since == null) {
            return countAll();
        }
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM feed_impressions WHERE created_at >= ?",
                Long.class,
                since);
        return count != null ? count : 0L;
    }

    @Override
    public long countByRankingProviderSince(String rankingProvider, LocalDateTime since) {
        Long count;
        if (since == null) {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM feed_impressions WHERE ranking_provider = ?",
                    Long.class,
                    rankingProvider);
        } else {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM feed_impressions WHERE ranking_provider = ? AND created_at >= ?",
                    Long.class,
                    rankingProvider,
                    since);
        }
        return count != null ? count : 0L;
    }
}
