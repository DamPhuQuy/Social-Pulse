-- Create posts table
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT,
    category_id BIGINT,
    location_id BIGINT,
    privacy_status VARCHAR DEFAULT 'PUBLIC',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    engagement_label VARCHAR DEFAULT 'NEW',

    CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_posts_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_posts_location FOREIGN KEY (location_id) REFERENCES cities(id) ON DELETE SET NULL,
    CONSTRAINT content_length CHECK (LENGTH(content) <= 5000)
);

-- Create indexes
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_category_id ON posts(category_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX idx_posts_privacy_status ON posts(privacy_status);
CREATE INDEX idx_posts_is_deleted ON posts(is_deleted);
CREATE INDEX idx_posts_engagement_label ON posts(engagement_label);
