CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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

INSERT INTO permissions (name, description) VALUES
    ('post:read', 'Read posts'),
    ('post:create', 'Create posts'),
    ('post:update', 'Update own posts'),
    ('post:delete', 'Delete own posts'),
    ('post:delete:any', 'Delete any posts'),
    ('comment:create', 'Create comments'),
    ('comment:update', 'Update own comments'),
    ('comment:delete', 'Delete own comments'),
    ('comment:delete:any', 'Delete any comments'),
    ('user:read', 'View user profiles'),
    ('user:update', 'Update own profile'),
    ('user:delete', 'Delete own account'),
    ('user:manage', 'Manage all users'),
    ('user:moderate', 'Moderate users')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;

INSERT INTO roles (name, description) VALUES
    ('GUEST', 'Guest user with read-only access'),
    ('USER', 'Regular authenticated user'),
    ('ADMIN', 'Administrator with full access')
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'post:read'
WHERE r.name = 'GUEST'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'post:read',
    'post:create',
    'post:update',
    'post:delete',
    'comment:create',
    'comment:update',
    'comment:delete',
    'user:read',
    'user:update',
    'user:delete'
)
WHERE r.name = 'USER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'post:read',
    'post:create',
    'post:update',
    'post:delete',
    'post:delete:any',
    'comment:create',
    'comment:update',
    'comment:delete',
    'comment:delete:any',
    'user:read',
    'user:update',
    'user:delete',
    'user:manage',
    'user:moderate'
)
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r
    ON r.name = COALESCE(NULLIF(u.role, ''), 'USER')
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'USER'
WHERE NOT EXISTS (
    SELECT 1
    FROM user_roles ur
    WHERE ur.user_id = u.id
)
ON CONFLICT DO NOTHING;
