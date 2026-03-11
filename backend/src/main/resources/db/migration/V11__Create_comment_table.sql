-- Create comment table
CREATE TABLE comment (
    id BIGSERIAL PRIMARY KEY,
    postid BIGINT NOT NULL,
    message VARCHAR(5000),
    createat TIMESTAMP DEFAULT NOW(),

    CONSTRAINT fk_comment_post FOREIGN KEY (postid) REFERENCES posts(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_comment_postid ON comment(postid);
CREATE INDEX idx_comment_createat ON comment(createat DESC);
