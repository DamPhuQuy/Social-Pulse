-- ============================================================
-- V2: Seed data — tạo user test để dùng ngay trên Swagger
-- ============================================================

-- pgcrypto: extension của PostgreSQL để hash password bằng bcrypt
-- crypt('Admin@123', gen_salt('bf', 12)) → bcrypt hash tương thích
-- với BCryptPasswordEncoder(12) của Spring Security
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Insert user test (chỉ insert nếu chưa tồn tại — idempotent)
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
    -- bcrypt hash của "Admin@123" với cost factor 12
    -- tương thích với PasswordEncoder(12) trong Java
    crypt('Admin@123', gen_salt('bf', 12)),
    'ACTIVE',      -- UserStatus.ACTIVE → có thể đăng nhập
    'USER',        -- UserRole.USER
    'VERIFIED',    -- VerificationStatus.VERIFIED → email đã xác thực
    false,
    0,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@socialpulse.com'
);

-- ============================================================
-- Thông tin đăng nhập để test trên Swagger:
--   Email:    admin@socialpulse.com
--   Password: Admin@123
-- ============================================================
