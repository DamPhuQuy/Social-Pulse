-- ============================================================
-- V3: Tạo bảng comments và comment_reactions
-- Tương ứng với entity Comment.java và CommentReaction.java
-- ============================================================

CREATE TABLE comments (
    id          BIGSERIAL PRIMARY KEY,
    post_id     BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    -- parent_id null = top-level comment; not null = reply
    parent_id   BIGINT      NULL,
    content     TEXT,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upvote_count    BIGINT  NOT NULL DEFAULT 0,
    down_vote_count BIGINT  NOT NULL DEFAULT 0,
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_comment_post
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent
        FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
);

CREATE INDEX idx_comment_post    ON comments(post_id);
CREATE INDEX idx_comment_user    ON comments(user_id);
CREATE INDEX idx_comment_parent  ON comments(parent_id);
CREATE INDEX idx_comment_created ON comments(created_at);

-- ============================================================

CREATE TABLE comment_reactions (
    id            BIGSERIAL PRIMARY KEY,
    comment_id    BIGINT      NOT NULL,
    user_id       BIGINT      NOT NULL,
    reaction_type VARCHAR(50) NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cmt_reaction_comment
        FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_cmt_reaction_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Mỗi user chỉ react 1 lần trên 1 comment
    CONSTRAINT uq_cmt_reaction UNIQUE (comment_id, user_id)
);

CREATE INDEX idx_comment_reaction_comment ON comment_reactions(comment_id);
CREATE INDEX idx_comment_reaction_user    ON comment_reactions(user_id);

-- ============================================================
-- post_reactions: user react vào post (like/dislike)
-- Tương ứng với entity PostReactions.java
-- ============================================================

CREATE TABLE post_reactions (
    id            BIGSERIAL PRIMARY KEY,
    post_id       BIGINT      NOT NULL,
    user_id       BIGINT      NOT NULL,
    reaction_type VARCHAR(50),
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_post_reaction_post
        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_reaction_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Mỗi user chỉ react 1 lần trên 1 post
    CONSTRAINT uq_post_reaction UNIQUE (post_id, user_id)
);

CREATE INDEX idx_post_reaction_post ON post_reactions(post_id);
CREATE INDEX idx_post_reaction_user ON post_reactions(user_id);
