-- Create search_history table
CREATE TABLE search_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_search_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_user_keyword UNIQUE (user_id, keyword)
);

-- Create indexes for better query performance
CREATE INDEX idx_user_keyword ON search_history(user_id, keyword);
CREATE INDEX idx_user_updated ON search_history(user_id, updated_at DESC);
