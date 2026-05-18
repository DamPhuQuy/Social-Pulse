CREATE TABLE user_interactions (
    id BIGSERIAL PRIMARY KEY,
    viewer_id BIGINT NOT NULL REFERENCES users(id),
    author_id BIGINT NOT NULL REFERENCES users(id),
    interaction_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_interactions_viewer_author ON user_interactions(viewer_id, author_id);
CREATE INDEX idx_user_interactions_viewer_created ON user_interactions(viewer_id, created_at DESC);
