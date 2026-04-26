-- Create permissions table
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create roles table
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create role_permissions junction table
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- Create user_roles junction table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Insert permissions with scope-based naming
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
    ('user:moderate', 'Moderate users');

-- Insert roles
INSERT INTO roles (name, description) VALUES
    ('GUEST', 'Guest user with read-only access'),
    ('USER', 'Regular authenticated user'),
    ('ADMIN', 'Administrator with full access');

-- Assign permissions to GUEST role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'GUEST' AND p.name IN ('post:read');

-- Assign permissions to USER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'USER' AND p.name IN (
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
);

-- Assign permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.name IN (
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
);

-- Migrate existing users to have USER role by default
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE r.name = 'USER' AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id
);

-- Drop the old role column from users table (if it exists)
-- Note: Uncomment this after verifying the migration works
-- ALTER TABLE users DROP COLUMN IF EXISTS role;
