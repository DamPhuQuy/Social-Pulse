package com.socialpulse.app.share.application.dto.response;

import java.time.LocalDateTime;

import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.enums.Privacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ShareCreationResponse {
    private Long id;
    private Long parentPostId;
    private Long userId;
    private String content;
    private Privacy privacy;
    private PostType type;
    private LocalDateTime createdAt;
}
