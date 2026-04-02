package com.socialpulse.app.post.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PostCreationResponse {
    private Long id;
    private String content;
    private String imageUrl;
    private Long userId;
    private LocalDateTime createdAt;
}
