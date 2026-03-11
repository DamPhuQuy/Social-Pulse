-- Create hashtags table
CREATE TABLE hashtags (
    id BIGSERIAL PRIMARY KEY,
    tag_name VARCHAR(30) UNIQUE NOT NULL
);

-- Create index
CREATE INDEX idx_hashtags_tag_name ON hashtags(tag_name);
