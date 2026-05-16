CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Seed the default administrator used for local testing.
INSERT INTO users (
    username,
    email,
    password_hash,
    status,
    role,
    verification,
    is_locked,
    failed_attempts,
    created_at,
    updated_at
)
SELECT
    'admin',
    'admin@socialpulse.com',
    crypt('Admin@123', gen_salt('bf', 12)),
    'ACTIVE',
    'ADMIN',
    'VERIFIED',
    false,
    0,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@socialpulse.com'
);
