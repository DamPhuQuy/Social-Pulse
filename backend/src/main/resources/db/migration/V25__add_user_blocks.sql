-- Create user_blocks table
CREATE TABLE user_blocks (
    id BIGSERIAL PRIMARY KEY,
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_blocker FOREIGN KEY (blocker_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_blocked FOREIGN KEY (blocked_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_block UNIQUE (blocker_id, blocked_id),
    CONSTRAINT check_not_self_block CHECK (blocker_id != blocked_id)
);

-- Create indexes for performance
CREATE INDEX idx_blocker ON user_blocks(blocker_id);
CREATE INDEX idx_blocked ON user_blocks(blocked_id);
