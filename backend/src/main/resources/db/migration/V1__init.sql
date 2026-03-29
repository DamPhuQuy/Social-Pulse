-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(72) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failed_attempts INT NOT NULL DEFAULT 0,
    lockout_until TIMESTAMP,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT username_length CHECK (LENGTH(username) BETWEEN 3 AND 20)
);

-- Create profiles table
CREATE TABLE profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    bio TEXT,
    dob DATE,
    gender VARCHAR(10),
    avatar_url VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT profiles_gender_check
        CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER'))
);

-- Create indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_status ON users(status);
