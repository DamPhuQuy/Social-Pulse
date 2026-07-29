CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- TABLES
-- ============================================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    verification VARCHAR(50) NOT NULL,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INT NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
    last_login_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE profiles (
    user_id BIGINT PRIMARY KEY,
    display_name VARCHAR(100),
    bio TEXT,
    dob DATE,
    gender VARCHAR(20),
    avatar_url VARCHAR(255),
    avatar_public_id VARCHAR(255),
    cover_image_url VARCHAR(2048),
    cover_image_public_id VARCHAR(255),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- posts.topic_id represents the primary topic category (single FK).
-- Multi-topic tags are managed separately in post_topics.
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    topic_id BIGINT,
    content TEXT,
    image_url TEXT,
    image_public_id VARCHAR(255),
    privacy VARCHAR(50) NOT NULL DEFAULT 'PUBLIC',
    type VARCHAR(20) NOT NULL DEFAULT 'ORIGINAL',
    parent_post_id BIGINT NULL,
    upvote_count BIGINT NOT NULL DEFAULT 0,
    downvote_count BIGINT NOT NULL DEFAULT 0,
    cmt_count BIGINT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    share_count BIGINT NOT NULL DEFAULT 0,
    hot_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    toxic BOOLEAN NOT NULL DEFAULT FALSE,
    toxic_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_posts_parent_post FOREIGN KEY (parent_post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_posts_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE SET NULL
);

CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT,
    edited BOOLEAN NOT NULL DEFAULT FALSE,
    upvote_count BIGINT NOT NULL DEFAULT 0,
    down_vote_count BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
);

CREATE TABLE comment_reactions (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cmt_reaction_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_cmt_reaction_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_cmt_reaction UNIQUE (comment_id, user_id)
);

CREATE TABLE post_reactions (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_post_reaction_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_reaction_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_post_reaction UNIQUE (post_id, user_id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ,
    replaced_by_token UUID,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_replaced_by FOREIGN KEY (replaced_by_token) REFERENCES refresh_tokens(id) ON DELETE SET NULL,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE follows (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_follower FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_following FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_follow UNIQUE (follower_id, following_id),
    CONSTRAINT check_not_self_follow CHECK (follower_id != following_id)
);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE bookmarks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_bookmarks_user_post UNIQUE (user_id, post_id)
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMPTZ NULL
);

CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    participant1_id BIGINT NOT NULL REFERENCES users(id),
    participant2_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMPTZ,
    CONSTRAINT uk_conversation_participants UNIQUE (participant1_id, participant2_id),
    CONSTRAINT chk_different_participants CHECK (participant1_id < participant2_id)
);

CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id),
    sender_id BIGINT NOT NULL REFERENCES users(id),
    content VARCHAR(2000) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(10) NOT NULL DEFAULT 'SENT',
    CONSTRAINT chk_message_status CHECK (status IN ('SENT', 'DELIVERED', 'READ'))
);

CREATE TABLE user_interactions (
    id BIGSERIAL PRIMARY KEY,
    viewer_id BIGINT NOT NULL REFERENCES users(id),
    author_id BIGINT NOT NULL REFERENCES users(id),
    interaction_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_topics (
    user_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, topic_id),
    CONSTRAINT fk_user_topics_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_topics_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
);

CREATE TABLE search_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_search_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_user_keyword UNIQUE (user_id, keyword)
);

-- Multiple topic tags per post, with FK constraint referencing topics(slug)
CREATE TABLE post_topics (
    post_id BIGINT NOT NULL,
    topic_order INT NOT NULL DEFAULT 0,
    topic_slug VARCHAR(255) NOT NULL,
    PRIMARY KEY (post_id, topic_order),
    CONSTRAINT fk_post_topics_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_topics_slug FOREIGN KEY (topic_slug) REFERENCES topics(slug) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE user_blocks (
    id BIGSERIAL PRIMARY KEY,
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_blocker FOREIGN KEY (blocker_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_blocked FOREIGN KEY (blocked_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_block UNIQUE (blocker_id, blocked_id),
    CONSTRAINT check_not_self_block CHECK (blocker_id != blocked_id)
);

CREATE TABLE feed_impressions (
    id BIGSERIAL PRIMARY KEY,
    viewer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    rank_position INT NOT NULL,
    page_number INT NOT NULL,
    page_size INT NOT NULL,
    ranking_score DOUBLE PRECISION,
    candidate_source VARCHAR(20),
    ranking_provider VARCHAR(20) NOT NULL,
    feature_schema_version VARCHAR(16) NOT NULL,
    feed_context VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_feed_impressions_ranking_provider CHECK (ranking_provider IN ('RULE_BASED', 'CHRONOLOGICAL', 'TOPIC', 'FALLBACK')),
    CONSTRAINT chk_feed_impressions_candidate_source CHECK (candidate_source IS NULL OR candidate_source IN ('RECENT', 'FOLLOWING', 'POPULAR', 'RANDOM', 'TOPIC')),
    CONSTRAINT chk_feed_impressions_ranking_score CHECK (ranking_score IS NULL OR ranking_score >= 0.0),
    CONSTRAINT chk_feed_impressions_positions CHECK (rank_position >= 0 AND page_number >= 0 AND page_size > 0)
);

CREATE TABLE topic_follows (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_slug VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_topic_follows UNIQUE (user_id, topic_slug),
    CONSTRAINT fk_topic_follows_slug FOREIGN KEY (topic_slug) REFERENCES topics(slug) ON DELETE CASCADE ON UPDATE CASCADE
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

CREATE INDEX idx_post_user ON posts(user_id);
CREATE INDEX idx_post_created ON posts(created_at);
CREATE INDEX idx_posts_parent_post_id ON posts(parent_post_id);
CREATE INDEX idx_posts_type ON posts(type);
CREATE INDEX idx_posts_topic ON posts(topic_id);
CREATE INDEX idx_posts_created_at_id ON posts(created_at DESC, id DESC);
CREATE INDEX idx_public_posts_feed ON posts (created_at DESC, id DESC) WHERE deleted_at IS NULL AND privacy = 'PUBLIC' AND toxic = false;

CREATE INDEX idx_topics_slug ON topics(slug);
CREATE INDEX idx_topics_slug_id ON topics(slug, id);

CREATE INDEX idx_comment_post ON comments(post_id);
CREATE INDEX idx_comment_user ON comments(user_id);
CREATE INDEX idx_comment_parent ON comments(parent_id);
CREATE INDEX idx_comment_created ON comments(created_at);

CREATE INDEX idx_comment_reaction_comment ON comment_reactions(comment_id);
CREATE INDEX idx_comment_reaction_user ON comment_reactions(user_id);

CREATE INDEX idx_post_reaction_post ON post_reactions(post_id);
CREATE INDEX idx_post_reaction_user ON post_reactions(user_id);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);

CREATE INDEX idx_report_reporter ON reports(reporter_id);
CREATE INDEX idx_report_target ON reports(target_type, target_id);
CREATE INDEX idx_report_status ON reports(status);
CREATE INDEX idx_report_created ON reports(created_at);

CREATE INDEX idx_follower ON follows(follower_id);
CREATE INDEX idx_following ON follows(following_id);
CREATE INDEX idx_follows_follower_following ON follows(follower_id, following_id);

CREATE INDEX idx_bookmarks_user ON bookmarks(user_id);
CREATE INDEX idx_bookmarks_post ON bookmarks(post_id);
CREATE INDEX idx_bookmarks_created ON bookmarks(created_at);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_id);
CREATE INDEX idx_notifications_actor ON notifications(actor_id);
CREATE INDEX idx_notifications_created ON notifications(created_at);
CREATE INDEX idx_notifications_unread ON notifications(recipient_id, read_at);

CREATE INDEX idx_conv_participant1 ON conversations(participant1_id);
CREATE INDEX idx_conv_participant2 ON conversations(participant2_id);
CREATE INDEX idx_conv_last_message ON conversations(last_message_at DESC);

CREATE INDEX idx_msg_conversation_ts ON messages(conversation_id, timestamp DESC);
CREATE INDEX idx_msg_sender ON messages(sender_id);
CREATE INDEX idx_msg_unread ON messages(conversation_id, status, sender_id) WHERE status != 'READ';

CREATE INDEX idx_user_interactions_viewer_author ON user_interactions(viewer_id, author_id);
CREATE INDEX idx_user_interactions_viewer_created ON user_interactions(viewer_id, created_at DESC);

CREATE INDEX idx_user_keyword ON search_history(user_id, keyword);
CREATE INDEX idx_user_updated ON search_history(user_id, updated_at DESC);

CREATE INDEX idx_post_topics_slug ON post_topics(topic_slug);

CREATE INDEX idx_blocker ON user_blocks(blocker_id);
CREATE INDEX idx_blocked ON user_blocks(blocked_id);

CREATE INDEX idx_feed_impressions_viewer_created ON feed_impressions(viewer_id, created_at DESC);
CREATE INDEX idx_feed_impressions_post_created ON feed_impressions(post_id, created_at DESC);
CREATE INDEX idx_feed_impressions_provider_created ON feed_impressions(ranking_provider, created_at DESC);

CREATE INDEX idx_topic_follows_user ON topic_follows(user_id);
CREATE INDEX idx_topic_follows_slug ON topic_follows(topic_slug);
CREATE INDEX idx_topic_follows_user_slug ON topic_follows(user_id, topic_slug);
