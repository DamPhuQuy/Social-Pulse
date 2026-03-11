-- Create post_media table
CREATE TABLE post_media (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    media_url VARCHAR NOT NULL,
    media_type VARCHAR,
    file_size BIGINT,

    CONSTRAINT fk_post_media_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_post_media_post_id ON post_media(post_id);
