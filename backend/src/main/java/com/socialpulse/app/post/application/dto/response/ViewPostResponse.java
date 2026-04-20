package com.socialpulse.app.post.application.dto.response;

import java.time.LocalDateTime;

import com.socialpulse.app.post.domain.enums.Privacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViewPostResponse {
    private String content;
    private String imageUrl;
    private String imagePublicId;
    private Privacy privacy;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
