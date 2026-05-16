CREATE TABLE IF NOT EXISTS bookmarks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_bookmarks_user_post UNIQUE (user_id, post_id)
);

CREATE INDEX IF NOT EXISTS idx_bookmarks_user ON bookmarks(user_id);
CREATE INDEX IF NOT EXISTS idx_bookmarks_post ON bookmarks(post_id);
CREATE INDEX IF NOT EXISTS idx_bookmarks_created ON bookmarks(created_at);

INSERT INTO permissions (name, description) VALUES
    ('discovery:read', 'Read discovery and search results'),
    ('bookmark:create', 'Create bookmarks'),
    ('bookmark:delete', 'Delete bookmarks'),
    ('bookmark:read', 'Read bookmarks')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'discovery:read',
    'bookmark:create',
    'bookmark:delete',
    'bookmark:read'
)
WHERE r.name IN ('USER', 'ADMIN')
ON CONFLICT DO NOTHING;
