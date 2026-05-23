CREATE TABLE feed_impressions (
    id BIGSERIAL PRIMARY KEY,
    viewer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    rank_position INT NOT NULL,
    page_number INT NOT NULL,
    page_size INT NOT NULL,
    ai_score DOUBLE PRECISION,
    candidate_source VARCHAR(20),
    ranking_provider VARCHAR(20) NOT NULL,
    feature_schema_version VARCHAR(16) NOT NULL,
    feed_context VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_feed_impressions_viewer_created ON feed_impressions(viewer_id, created_at DESC);
CREATE INDEX idx_feed_impressions_post_created ON feed_impressions(post_id, created_at DESC);
CREATE INDEX idx_feed_impressions_provider_created ON feed_impressions(ranking_provider, created_at DESC);
