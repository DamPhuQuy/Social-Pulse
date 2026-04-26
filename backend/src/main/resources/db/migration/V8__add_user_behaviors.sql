-- User behavior tracking table for ML feature extraction
CREATE TABLE user_behaviors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    position INT,
    dwell_time_seconds INT,
    metadata JSONB,

    CONSTRAINT fk_user_behaviors_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_behaviors_post
        FOREIGN KEY (post_id)
        REFERENCES posts(id)
        ON DELETE CASCADE
);

-- Indexes for efficient querying
CREATE INDEX idx_user_behaviors_user_time ON user_behaviors(user_id, event_time DESC);
CREATE INDEX idx_user_behaviors_post_time ON user_behaviors(post_id, event_time DESC);
CREATE INDEX idx_user_behaviors_event_type ON user_behaviors(event_type);
CREATE INDEX idx_user_behaviors_user_post ON user_behaviors(user_id, post_id);

-- Index for time-based aggregations (7d, 30d windows)
CREATE INDEX idx_user_behaviors_time ON user_behaviors(event_time DESC);

-- Composite index for user-author interaction queries
CREATE INDEX idx_user_behaviors_user_event_time ON user_behaviors(user_id, event_type, event_time DESC);
