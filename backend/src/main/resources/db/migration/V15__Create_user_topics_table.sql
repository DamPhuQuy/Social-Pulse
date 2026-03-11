-- Create user_topics table
CREATE TABLE user_topics (
    user_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, topic_id),
    CONSTRAINT fk_user_topics_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_topics_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_user_topics_user_id ON user_topics(user_id);
CREATE INDEX idx_user_topics_topic_id ON user_topics(topic_id);
