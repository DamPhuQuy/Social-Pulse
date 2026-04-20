package com.socialpulse.app.post.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostReactionResponse {
    private Long id;
    private Long userId;
    private Long postId;
    private String reactionType;
}
