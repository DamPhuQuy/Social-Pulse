package com.socialpulse.app.post.dto.response;

import java.time.LocalDateTime;

import com.socialpulse.app.post.entity.Privacy;

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
    private Long userId;
    private Privacy privacy;
    private LocalDateTime createdAt;
}
