-- Create topics table
CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create user_topics mapping table
CREATE TABLE user_topics (
    user_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, topic_id),
    CONSTRAINT fk_user_topics_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_topics_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
);

-- Add topic_id to posts table
ALTER TABLE posts ADD COLUMN topic_id BIGINT;
ALTER TABLE posts ADD CONSTRAINT fk_posts_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE SET NULL;
CREATE INDEX idx_posts_topic ON posts(topic_id);

-- Insert predefined topics
INSERT INTO topics (name, slug) VALUES
('Công nghệ', 'cong-nghe'),
('Gaming', 'gaming'),
('Phim ảnh', 'phim-anh'),
('Âm nhạc', 'am-nhac'),
('Thể thao', 'the-thao'),
('Ẩm thực', 'am-thuc'),
('Du lịch', 'du-lich'),
('Thời trang', 'thoi-trang'),
('Sức khỏe', 'suc-khoe'),
('Giáo dục', 'giao-duc'),
('Tài chính', 'tai-chinh'),
('Nghệ thuật', 'nghe-thuat'),
('Thú cưng', 'thu-cung'),
('Hài hước', 'hai-huoc'),
('Tâm sự', 'tam-su'),
('Hỏi đáp', 'hoi-dap'),
('Khoa học', 'khoa-hoc'),
('Anime', 'anime'),
('Sách', 'sach'),
('Nhiếp ảnh', 'nhiep-anh');

-- Update existing posts to a default topic
UPDATE posts SET topic_id = (SELECT id FROM topics WHERE slug = 'hoi-dap') WHERE topic_id IS NULL;

