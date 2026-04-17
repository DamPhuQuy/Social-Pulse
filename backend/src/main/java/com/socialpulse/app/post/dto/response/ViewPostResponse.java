package com.socialpulse.app.post.dto.response;

import java.time.LocalDateTime;

import com.socialpulse.app.post.entity.Privacy;
import com.socialpulse.app.user.entity.User;

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
