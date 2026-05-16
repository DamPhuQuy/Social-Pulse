ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(100);

UPDATE profiles p
SET display_name = u.username
FROM users u
WHERE p.user_id = u.id
  AND (p.display_name IS NULL OR p.display_name = '');

INSERT INTO profiles (user_id, display_name, updated_at)
SELECT u.id, u.username, CURRENT_TIMESTAMP
FROM users u
WHERE NOT EXISTS (
    SELECT 1
    FROM profiles p
    WHERE p.user_id = u.id
);

INSERT INTO permissions (name, description) VALUES
    ('user:create', 'Create own profile'),
    ('post:react', 'React to posts'),
    ('post:manage', 'Manage any post'),
    ('comment:read', 'Read comments'),
    ('comment:manage', 'Manage any comment'),
    ('follow:create', 'Follow users'),
    ('follow:delete', 'Unfollow users'),
    ('feed:read', 'Read personalized feed'),
    ('report:create', 'Create reports')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'user:create',
    'post:react',
    'comment:read',
    'follow:create',
    'follow:delete',
    'feed:read',
    'report:create'
)
WHERE r.name = 'USER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'user:create',
    'post:react',
    'post:manage',
    'comment:read',
    'comment:manage',
    'follow:create',
    'follow:delete',
    'feed:read',
    'report:create'
)
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;
