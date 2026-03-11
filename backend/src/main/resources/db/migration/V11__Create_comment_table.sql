-- Create comment table
CREATE TABLE comment (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    message VARCHAR(5000),
    created_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_comment_post_id ON comment(post_id);
CREATE INDEX idx_comment_created_at ON comment(created_at DESC);
