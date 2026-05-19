package com.socialpulse.app.post.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.enums.Privacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostUpdateResponse {
    private Long id;
    private Long userId;
    private String content;
    private String imageUrl;
    private String imagePublicId;
    private List<String> topicSlugs;
    private PostType type;
    private Privacy privacy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
