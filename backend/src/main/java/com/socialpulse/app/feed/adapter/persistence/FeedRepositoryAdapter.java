package com.socialpulse.app.feed.adapter.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.feed.domain.repository.FeedRepository;

public class FeedRepositoryAdapter implements FeedRepository {
    private final JdbcTemplate jdbcTemplate;

    public FeedRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Post> postRowMapper = (rs, rowNum) -> Post.builder()
            .id(rs.getLong("id"))
            .userId(rs.getLong("user_id"))
            .content(rs.getString("content"))
            .imageUrl(rs.getString("image_url"))
            .imagePublicId(rs.getString("image_public_id"))
            .parentPostId(rs.getObject("parent_post_id", Long.class))
            .type(PostType.valueOf(rs.getString("type")))
            .privacy(Privacy.valueOf(rs.getString("privacy")))
            .upvoteCount(rs.getLong("upvote_count"))
            .downvoteCount(rs.getLong("downvote_count"))
            .cmtCount(rs.getLong("cmt_count"))
            .viewCount(rs.getLong("view_count"))
            .shareCount(rs.getLong("share_count"))
            .hotScore(rs.getDouble("hot_score"))
            .toxic(rs.getBoolean("toxic"))
            .toxicScore(rs.getDouble("toxic_score"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .deletedAt(rs.getTimestamp("deleted_at") != null ? rs.getTimestamp("deleted_at").toLocalDateTime() : null)
            .build();

    @Override
    public List<Post> findRecentPosts(LocalDateTime since, Pageable pageable) {
        String sql = """
            SELECT * FROM posts
            WHERE deleted_at IS NULL
              AND privacy = 'PUBLIC'
              AND toxic = false
              AND created_at >= ?
            ORDER BY created_at DESC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, postRowMapper, since, pageable.getPageSize());
    }

    @Override
    public List<Post> findFollowingPosts(Long userId, LocalDateTime since, Pageable pageable) {
        String sql = """
            SELECT p.* FROM posts p
            INNER JOIN follows f ON p.user_id = f.following_id
            WHERE f.follower_id = ?
              AND p.deleted_at IS NULL
              AND p.privacy = 'PUBLIC'
              AND p.toxic = false
              AND p.created_at >= ?
            ORDER BY p.created_at DESC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, postRowMapper, userId, since, pageable.getPageSize());
    }

    @Override
    public List<Post> findPopularPosts(LocalDateTime since, Pageable pageable) {
        String sql = """
            SELECT * FROM posts
            WHERE deleted_at IS NULL
              AND privacy = 'PUBLIC'
              AND toxic = false
              AND created_at >= ?
            ORDER BY hot_score DESC, upvote_count DESC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, postRowMapper, since, pageable.getPageSize());
    }

    @Override
    public List<Post> findRandomPosts(List<Long> excludeIds, Pageable pageable) {
        String sql;
        Object[] params;

        if (excludeIds == null || excludeIds.isEmpty()) {
            sql = """
                SELECT * FROM posts
                WHERE deleted_at IS NULL
                  AND privacy = 'PUBLIC'
                  AND toxic = false
                ORDER BY RANDOM()
                LIMIT ?
                """;
            params = new Object[]{pageable.getPageSize()};
        } else {
            String placeholders = String.join(",", excludeIds.stream().map(id -> "?").toList());
            sql = String.format("""
                SELECT * FROM posts
                WHERE deleted_at IS NULL
                  AND privacy = 'PUBLIC'
                  AND toxic = false
                  AND id NOT IN (%s)
                ORDER BY RANDOM()
                LIMIT ?
                """, placeholders);
            params = new Object[excludeIds.size() + 1];
            for (int i = 0; i < excludeIds.size(); i++) {
                params[i] = excludeIds.get(i);
            }
            params[excludeIds.size()] = pageable.getPageSize();
        }

        return jdbcTemplate.query(sql, postRowMapper, params);
    }
}
