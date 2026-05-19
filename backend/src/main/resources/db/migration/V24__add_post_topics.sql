CREATE TABLE IF NOT EXISTS post_topics (
    post_id BIGINT NOT NULL,
    topic_order INT NOT NULL DEFAULT 0,
    topic_slug VARCHAR(80) NOT NULL,
    PRIMARY KEY (post_id, topic_order),
    CONSTRAINT fk_post_topics_post
        FOREIGN KEY (post_id)
        REFERENCES posts(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_post_topics_slug ON post_topics(topic_slug);

INSERT INTO post_topics (post_id, topic_order, topic_slug)
SELECT
    p.id,
    0,
    CASE
        WHEN LOWER(COALESCE(p.content, '')) LIKE '%backend%'
          OR LOWER(COALESCE(p.content, '')) LIKE '%api%'
          OR LOWER(COALESCE(p.content, '')) LIKE '%react%'
          OR LOWER(COALESCE(p.content, '')) LIKE '%frontend%'
          OR LOWER(COALESCE(p.content, '')) LIKE '%tech%'
          THEN 'technology'
        WHEN LOWER(COALESCE(p.content, '')) LIKE '%onboarding%'
          OR LOWER(COALESCE(p.content, '')) LIKE '%flow%'
          THEN 'design'
        WHEN LOWER(COALESCE(p.content, '')) LIKE '%topic bucket%'
          THEN 'community'
        ELSE 'general'
    END
FROM posts p
WHERE NOT EXISTS (
    SELECT 1
    FROM post_topics pt
    WHERE pt.post_id = p.id
);
