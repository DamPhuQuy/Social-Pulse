package com.socialpulse.app.feed.adapter.persistence;

import java.time.LocalDateTime;

import org.springframework.jdbc.core.JdbcTemplate;

import com.socialpulse.app.feed.domain.repository.UserInteractionRepository;

public class UserInteractionRepositoryAdapter implements UserInteractionRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserInteractionRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long countByViewerAndAuthorSince(Long viewerId, Long authorId, LocalDateTime since) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_interactions WHERE viewer_id = ? AND author_id = ? AND created_at >= ?",
                Long.class, viewerId, authorId, since);
        return count != null ? count : 0L;
    }

    @Override
    public LocalDateTime findLatestInteractionTime(Long viewerId, Long authorId) {
        return jdbcTemplate.query(
                "SELECT created_at FROM user_interactions WHERE viewer_id = ? AND author_id = ? ORDER BY created_at DESC LIMIT 1",
                rs -> rs.next() ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                viewerId, authorId);
    }

    @Override
    public long countTotalByViewerSince(Long viewerId, LocalDateTime since) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_interactions WHERE viewer_id = ? AND created_at >= ?",
                Long.class, viewerId, since);
        return count != null ? count : 0L;
    }

    @Override
    public void save(Long viewerId, Long authorId, String interactionType) {
        jdbcTemplate.update(
                "INSERT INTO user_interactions (viewer_id, author_id, interaction_type, created_at) VALUES (?, ?, ?, NOW())",
                viewerId, authorId, interactionType);
    }
}
