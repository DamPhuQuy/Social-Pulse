-- ============================================================
-- V5: Align DevPulse Post Schema (Daily.dev Model) & Bookmarks
-- ============================================================

-- 1. Add Developer Article Primitives to posts table
ALTER TABLE posts ADD COLUMN IF NOT EXISTS title VARCHAR(255);
ALTER TABLE posts ADD COLUMN IF NOT EXISTS source_url VARCHAR(2048);
ALTER TABLE posts ADD COLUMN IF NOT EXISTS source_domain VARCHAR(100);
ALTER TABLE posts ADD COLUMN IF NOT EXISTS summary TEXT;

-- 2. Backfill existing records with clean default titles from content
UPDATE posts
SET title = CASE 
    WHEN content IS NOT NULL AND LENGTH(TRIM(content)) > 0 THEN 
        CASE 
            WHEN LENGTH(TRIM(content)) <= 60 THEN TRIM(content)
            ELSE SUBSTRING(TRIM(content) FROM 1 FOR 57) || '...'
        END
    ELSE 'Technical Discussion #' || id
END
WHERE title IS NULL;

-- Ensure title is not null for future integrity
ALTER TABLE posts ALTER COLUMN title SET NOT NULL;

-- 3. Re-create lean Bookmarks (Reading List) table
CREATE TABLE IF NOT EXISTS bookmarks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_bookmark UNIQUE (user_id, post_id)
);

CREATE INDEX IF NOT EXISTS idx_bookmarks_user ON bookmarks(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bookmarks_post ON bookmarks(post_id);
