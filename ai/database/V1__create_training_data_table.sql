-- Migration: Create feed training data table for AI model training
-- Purpose: Store feature vectors and labels for supervised learning
-- Expected size: 100K-1M rows for production model

CREATE TABLE IF NOT EXISTS feed_training_data (
    id BIGSERIAL PRIMARY KEY,

    -- Identifiers
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,

    -- Content-based features (7 features)
    keywords_relevance DOUBLE PRECISION DEFAULT 0.0,      -- 0-1000: keyword matching score
    hashtags_relevance DOUBLE PRECISION DEFAULT 0.0,      -- 0-1000: hashtag matching score
    mentions_relevance INTEGER DEFAULT 0,                 -- 0/1: binary mention indicator
    content_length INTEGER DEFAULT 0,                     -- 0-5000: character count
    has_hashtags INTEGER DEFAULT 0,                       -- 0/1: contains hashtags
    has_url INTEGER DEFAULT 0,                            -- 0/1: contains URLs
    has_multimedia INTEGER DEFAULT 0,                     -- 0/1: has image/video

    -- Author-based features (8 features)
    interaction_rate DOUBLE PRECISION DEFAULT 0.0,        -- 0.0-1.0: historical interaction rate
    mention_count INTEGER DEFAULT 0,                      -- 0-1000: times author mentioned user
    followers_followings_ratio DOUBLE PRECISION DEFAULT 0.0, -- 0-1M: author influence metric
    author_seniority DOUBLE PRECISION DEFAULT 0.0,        -- 0-20: account age in years
    author_follower_count INTEGER DEFAULT 0,              -- raw follower count
    author_following_count INTEGER DEFAULT 0,             -- raw following count
    author_post_count INTEGER DEFAULT 0,                  -- total posts by author
    author_engagement_rate DOUBLE PRECISION DEFAULT 0.0,  -- 0-1: author's avg engagement

    -- Relationship features (5 features)
    follows INTEGER DEFAULT 0,                            -- 0/1: user follows author
    interaction_count_7d INTEGER DEFAULT 0,               -- interactions in last 7 days
    interaction_count_30d INTEGER DEFAULT 0,              -- interactions in last 30 days
    hours_since_last_interaction DOUBLE PRECISION DEFAULT 999.0, -- recency metric
    affinity_score DOUBLE PRECISION DEFAULT 0.0,          -- weighted affinity score

    -- Engagement features (6 features)
    popularity BIGINT DEFAULT 0,                          -- total engagement count
    upvote_count BIGINT DEFAULT 0,                        -- upvotes at impression time
    downvote_count BIGINT DEFAULT 0,                      -- downvotes at impression time
    comment_count BIGINT DEFAULT 0,                       -- comments at impression time
    share_count BIGINT DEFAULT 0,                         -- shares at impression time
    view_count BIGINT DEFAULT 0,                          -- views at impression time

    -- Target variable (label)
    relevance INTEGER NOT NULL,                           -- 0 or 1 (0=no interaction, 1=interacted)

    -- Additional context features (not used in model, but useful for analysis)
    post_type VARCHAR(50),                                -- ORIGINAL, SHARE, etc.
    post_created_at TIMESTAMP,                            -- when post was created
    recency_hours DOUBLE PRECISION,                       -- hours since post creation
    position_in_feed INTEGER,                             -- display position (for bias correction)

    -- Metadata
    impression_time TIMESTAMP NOT NULL,                   -- when user saw the post
    interaction_time TIMESTAMP,                           -- when user interacted (NULL if no interaction)
    interaction_type VARCHAR(50),                         -- UPVOTE, COMMENT, SHARE, etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_relevance CHECK (relevance IN (0, 1)),
    CONSTRAINT chk_binary_features CHECK (
        mentions_relevance IN (0, 1) AND
        has_hashtags IN (0, 1) AND
        has_url IN (0, 1) AND
        has_multimedia IN (0, 1) AND
        follows IN (0, 1)
    )
);

-- Indexes for efficient querying
CREATE INDEX idx_training_user_id ON feed_training_data(user_id);
CREATE INDEX idx_training_post_id ON feed_training_data(post_id);
CREATE INDEX idx_training_author_id ON feed_training_data(author_id);
CREATE INDEX idx_training_created_at ON feed_training_data(created_at DESC);
CREATE INDEX idx_training_impression_time ON feed_training_data(impression_time DESC);
CREATE INDEX idx_training_relevance ON feed_training_data(relevance);

-- Composite index for user-specific queries
CREATE INDEX idx_training_user_time ON feed_training_data(user_id, impression_time DESC);

-- Index for data export queries
CREATE INDEX idx_training_export ON feed_training_data(created_at, user_id)
    WHERE relevance IS NOT NULL;

-- Comments
COMMENT ON TABLE feed_training_data IS 'Training data for AI-powered feed ranking model';
COMMENT ON COLUMN feed_training_data.relevance IS 'Target variable: 0=user did not interact, 1=user interacted (upvote/comment/share)';
COMMENT ON COLUMN feed_training_data.keywords_relevance IS 'Cosine similarity between post keywords and user interest profile (0-1000)';
COMMENT ON COLUMN feed_training_data.interaction_rate IS 'Historical interaction rate between user and author (0.0-1.0)';
COMMENT ON COLUMN feed_training_data.affinity_score IS 'Time-decayed weighted affinity score based on past interactions';

-- Create table for user interest profiles (cached)
CREATE TABLE IF NOT EXISTS user_interest_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,

    -- Keyword profile (JSON: {keyword: score})
    keyword_profile JSONB,

    -- Hashtag profile (JSON: {hashtag: score})
    hashtag_profile JSONB,

    -- Metadata
    profile_size INTEGER DEFAULT 0,                       -- number of keywords/hashtags
    last_interaction_time TIMESTAMP,                      -- last time user interacted with content
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_interest_user_id ON user_interest_profiles(user_id);
CREATE INDEX idx_interest_updated_at ON user_interest_profiles(updated_at DESC);

COMMENT ON TABLE user_interest_profiles IS 'Cached user interest profiles for content-based filtering';

-- Create table for tracking impression events (before we know if user will interact)
CREATE TABLE IF NOT EXISTS feed_impressions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,

    position_in_feed INTEGER NOT NULL,                    -- display position (0-indexed)
    ranking_strategy VARCHAR(50),                         -- CHRONOLOGICAL, HOT_SCORE, AI_RANKING, etc.

    impression_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Track if user interacted (updated later)
    interacted BOOLEAN DEFAULT FALSE,
    interaction_time TIMESTAMP,
    interaction_type VARCHAR(50),

    -- For deduplication
    UNIQUE(user_id, post_id, impression_time)
);

CREATE INDEX idx_impression_user_post ON feed_impressions(user_id, post_id);
CREATE INDEX idx_impression_time ON feed_impressions(impression_time DESC);
CREATE INDEX idx_impression_user_time ON feed_impressions(user_id, impression_time DESC);

COMMENT ON TABLE feed_impressions IS 'Track when users see posts in their feed (for training data collection)';

-- Create materialized view for quick training data export
CREATE MATERIALIZED VIEW training_data_summary AS
SELECT
    DATE(created_at) as date,
    COUNT(*) as total_samples,
    SUM(CASE WHEN relevance = 1 THEN 1 ELSE 0 END) as positive_samples,
    SUM(CASE WHEN relevance = 0 THEN 1 ELSE 0 END) as negative_samples,
    ROUND(AVG(CASE WHEN relevance = 1 THEN 1.0 ELSE 0.0 END)::numeric, 3) as positive_rate,
    COUNT(DISTINCT user_id) as unique_users,
    COUNT(DISTINCT post_id) as unique_posts
FROM feed_training_data
GROUP BY DATE(created_at)
ORDER BY date DESC;

CREATE UNIQUE INDEX idx_training_summary_date ON training_data_summary(date);

COMMENT ON MATERIALIZED VIEW training_data_summary IS 'Daily summary of training data collection (refresh periodically)';

-- Function to refresh the summary view
CREATE OR REPLACE FUNCTION refresh_training_summary()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY training_data_summary;
END;
$$ LANGUAGE plpgsql;

-- Create function to calculate interaction rate for a user-author pair
CREATE OR REPLACE FUNCTION calculate_interaction_rate(
    p_user_id BIGINT,
    p_author_id BIGINT,
    p_lookback_days INTEGER DEFAULT 30
)
RETURNS DOUBLE PRECISION AS $$
DECLARE
    v_total_impressions INTEGER;
    v_total_interactions INTEGER;
    v_rate DOUBLE PRECISION;
BEGIN
    -- Count impressions and interactions in lookback period
    SELECT
        COUNT(*) as total,
        SUM(CASE WHEN interacted THEN 1 ELSE 0 END) as interactions
    INTO v_total_impressions, v_total_interactions
    FROM feed_impressions
    WHERE user_id = p_user_id
        AND author_id = p_author_id
        AND impression_time >= NOW() - (p_lookback_days || ' days')::INTERVAL;

    IF v_total_impressions = 0 THEN
        RETURN 0.0;
    END IF;

    v_rate := v_total_interactions::DOUBLE PRECISION / v_total_impressions::DOUBLE PRECISION;
    RETURN v_rate;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION calculate_interaction_rate IS 'Calculate historical interaction rate between user and author';
