package com.socialpulse.app.post.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.enums.Privacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PostCreationResponse {
    private Long id;
    private String content;
    private String imageUrl;
    private String imagePublicId;
    private List<String> topicSlugs;
    private Long parentPostId;
    private PostType type;
    private Long userId;
    private Privacy privacy;
    private LocalDateTime createdAt;
}
