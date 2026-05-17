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
('Văn hóa Internet', 'van-hoa-internet'),
('Trò chơi', 'tro-choi'),
('Hỏi & Đáp & Câu chuyện', 'hoi-dap-cau-chuyen'),
('Phim & TV', 'phim-tv'),
('Công nghệ', 'cong-nghe'),
('Địa điểm và du lịch', 'dia-diem-du-lich'),
('Văn hóa đại chúng', 'van-hoa-dai-chung'),
('Thể thao', 'the-thao'),
('Kinh doanh và tài chính', 'kinh-doanh-tai-chinh'),
('Tin tức và chính trị', 'tin-tuc-chinh-tri'),
('Giáo dục và nghề nghiệp', 'giao-duc-nghe-nghiep'),
('Thời trang và làm đẹp', 'thoi-trang-lam-dep'),
('Xe cộ', 'xe-co'),
('Nhà cửa và vườn tược', 'nha-cua-vuon-tuoc'),
('Âm nhạc', 'am-nhac'),
('Ẩm thực', 'am-thuc'),
('Anime và Cosplay', 'anime-cosplay'),
('Nhân đạo và luật pháp', 'nhan-dao-luat-phap'),
('Khoa học', 'khoa-hoc'),
('Đọc và viết', 'doc-va-viet'),
('Nghệ thuật', 'nghe-thuat'),
('Sức khỏe', 'suc-khoe'),
('Đồ sưu tầm và sở thích khác', 'do-suu-tam-so-thich-khac'),
('Ma quái', 'ma-quai'),
('Thiên nhiên và ngoài trời', 'thien-nhien-ngoai-troi');

-- Update existing posts to a default topic (e.g., 'Văn hóa đại chúng' or 'Hỏi & Đáp & Câu chuyện')
-- Let's use 'Hỏi & Đáp & Câu chuyện' (id=3) for all old posts to prevent nulls if we want to enforce it.
UPDATE posts SET topic_id = (SELECT id FROM topics WHERE slug = 'hoi-dap-cau-chuyen') WHERE topic_id IS NULL;

-- If you want it to be mandatory in DB, uncomment this:
-- ALTER TABLE posts ALTER COLUMN topic_id SET NOT NULL;
