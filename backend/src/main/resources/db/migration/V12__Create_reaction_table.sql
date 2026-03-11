-- Create reaction table
CREATE TABLE reaction (
    id BIGSERIAL PRIMARY KEY,
    userid BIGINT UNIQUE NOT NULL,
    postid BIGINT NOT NULL,
    reactiontype BOOLEAN,

    CONSTRAINT fk_reaction_user FOREIGN KEY (userid) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reaction_post FOREIGN KEY (postid) REFERENCES posts(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_reaction_userid ON reaction(userid);
CREATE INDEX idx_reaction_postid ON reaction(postid);
CREATE INDEX idx_reaction_type ON reaction(reactiontype);
