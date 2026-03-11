-- Create reactioncmt table
CREATE TABLE reactioncmt (
    id BIGSERIAL PRIMARY KEY,
    cmt_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reactiontype BOOLEAN,

    CONSTRAINT fk_reactioncmt_comment FOREIGN KEY (cmt_id) REFERENCES comment(id) ON DELETE CASCADE,
    CONSTRAINT fk_reactioncmt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_reactioncmt_user_cmt UNIQUE (user_id, cmt_id)
);

-- Create indexes
CREATE INDEX idx_reactioncmt_cmt_id ON reactioncmt(cmt_id);
CREATE INDEX idx_reactioncmt_user_id ON reactioncmt(user_id);
CREATE INDEX idx_reactioncmt_type ON reactioncmt(reactiontype);
