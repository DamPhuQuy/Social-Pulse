package com.socialpulse.app.post.application.dto.response;

import com.socialpulse.app.common.utils.ReactionType;

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
    private Long postId;
    private Long userId;
    private ReactionType reactionType;
}
