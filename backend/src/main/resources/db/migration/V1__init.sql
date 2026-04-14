CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    role VARCHAR(50) NOT NULL,
    verification VARCHAR(50) NOT NULL,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INT NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE profiles (
    user_id BIGINT PRIMARY KEY,
    bio TEXT,
    dob DATE,
    gender VARCHAR(20),
    avatar_url VARCHAR(255),
    avatar_public_id VARCHAR(255),
    updated_at TIMESTAMP,
    CONSTRAINT fk_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT,
    image_url VARCHAR(255),
    image_public_id VARCHAR(255),
    privacy VARCHAR(50) NOT NULL DEFAULT 'PUBLIC',
    upvote_count BIGINT NOT NULL DEFAULT 0,
    downvote_count BIGINT NOT NULL DEFAULT 0,
    cmt_count BIGINT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    share_count BIGINT NOT NULL DEFAULT 0,
    hot_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    toxic BOOLEAN NOT NULL DEFAULT FALSE,
    toxic_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_posts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_post_user ON posts(user_id);
CREATE INDEX idx_post_created ON posts(created_at);
