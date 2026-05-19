package com.socialpulse.app.post.application.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.post.application.dto.response.PostTopicResponse;

public final class PostTopicCatalog {
    private static final Map<String, PostTopicResponse> TOPICS = buildTopics();

    private PostTopicCatalog() {
    }

    public static List<PostTopicResponse> all() {
        return List.copyOf(TOPICS.values());
    }

    public static List<String> normalizeAndValidate(List<String> topicSlugs) {
        if (topicSlugs == null || topicSlugs.isEmpty() || topicSlugs.size() > 5) {
            throw new AppException(PostCode.POST_TOPIC_INVALID);
        }

        List<String> normalized = topicSlugs.stream()
                .map(PostTopicCatalog::normalize)
                .distinct()
                .toList();

        if (normalized.isEmpty() || normalized.size() > 5 || normalized.stream().anyMatch(slug -> !TOPICS.containsKey(slug))) {
            throw new AppException(PostCode.POST_TOPIC_INVALID);
        }

        return normalized;
    }

    private static String normalize(String slug) {
        return slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, PostTopicResponse> buildTopics() {
        Map<String, PostTopicResponse> topics = new LinkedHashMap<>();
        add(topics, "daily-life", "Đời sống hằng ngày", "Đời sống");
        add(topics, "family", "Gia đình", "Đời sống");
        add(topics, "relationships", "Tình yêu & các mối quan hệ", "Đời sống");
        add(topics, "friends", "Bạn bè", "Đời sống");
        add(topics, "personal-growth", "Phát triển bản thân", "Đời sống");
        add(topics, "mental-health", "Sức khỏe tinh thần", "Sức khỏe");
        add(topics, "fitness", "Thể hình", "Sức khỏe");
        add(topics, "nutrition", "Dinh dưỡng", "Sức khỏe");
        add(topics, "medicine", "Y tế", "Sức khỏe");
        add(topics, "education", "Giáo dục", "Học tập");
        add(topics, "study", "Học tập", "Học tập");
        add(topics, "career", "Sự nghiệp", "Công việc");
        add(topics, "workplace", "Nơi làm việc", "Công việc");
        add(topics, "business", "Kinh doanh", "Kinh tế");
        add(topics, "finance", "Tài chính cá nhân", "Kinh tế");
        add(topics, "investment", "Đầu tư", "Kinh tế");
        add(topics, "technology", "Công nghệ", "Công nghệ");
        add(topics, "programming", "Lập trình", "Công nghệ");
        add(topics, "ai", "AI", "Công nghệ");
        add(topics, "science", "Khoa học", "Tri thức");
        add(topics, "environment", "Môi trường", "Xã hội");
        add(topics, "news", "Tin tức", "Xã hội");
        add(topics, "politics", "Chính trị", "Xã hội");
        add(topics, "community", "Cộng đồng", "Xã hội");
        add(topics, "culture", "Văn hóa", "Văn hóa");
        add(topics, "history", "Lịch sử", "Văn hóa");
        add(topics, "books", "Sách", "Giải trí");
        add(topics, "movies", "Phim ảnh", "Giải trí");
        add(topics, "music", "Âm nhạc", "Giải trí");
        add(topics, "gaming", "Game", "Giải trí");
        add(topics, "sports", "Thể thao", "Giải trí");
        add(topics, "football", "Bóng đá", "Giải trí");
        add(topics, "food", "Ẩm thực", "Phong cách sống");
        add(topics, "travel", "Du lịch", "Phong cách sống");
        add(topics, "fashion", "Thời trang", "Phong cách sống");
        add(topics, "beauty", "Làm đẹp", "Phong cách sống");
        add(topics, "home", "Nhà cửa", "Phong cách sống");
        add(topics, "pets", "Thú cưng", "Phong cách sống");
        add(topics, "parenting", "Nuôi dạy con", "Đời sống");
        add(topics, "art", "Nghệ thuật", "Sáng tạo");
        add(topics, "design", "Thiết kế", "Sáng tạo");
        add(topics, "photography", "Nhiếp ảnh", "Sáng tạo");
        add(topics, "writing", "Viết lách", "Sáng tạo");
        add(topics, "cars", "Xe cộ", "Sở thích");
        add(topics, "shopping", "Mua sắm", "Phong cách sống");
        add(topics, "humor", "Hài hước", "Giải trí");
        add(topics, "events", "Sự kiện", "Xã hội");
        add(topics, "general", "Chủ đề chung", "Khác");
        return topics;
    }

    private static void add(Map<String, PostTopicResponse> topics, String slug, String label, String category) {
        topics.put(slug, PostTopicResponse.builder()
                .slug(slug)
                .label(label)
                .category(category)
                .build());
    }
}
