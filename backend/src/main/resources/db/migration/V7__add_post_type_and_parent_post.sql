-- ============================================================
-- V7: Hỗ trợ share theo mô hình Reddit (share là một loại post)
-- ============================================================

ALTER TABLE posts
    ADD COLUMN parent_post_id BIGINT NULL;

ALTER TABLE posts
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'ORIGINAL';

ALTER TABLE posts
    ADD CONSTRAINT fk_posts_parent_post
        FOREIGN KEY (parent_post_id)
        REFERENCES posts(id)
        ON DELETE CASCADE;

CREATE INDEX idx_posts_parent_post_id ON posts(parent_post_id);
CREATE INDEX idx_posts_type ON posts(type);
