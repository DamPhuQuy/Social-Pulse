package com.socialpulse.app.comment.application.dto.response;

import com.socialpulse.app.common.utils.ReactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentReactionResponse {
    private Long id;
    private Long commentId;
    private Long userId;
    private ReactionType reactionType;
}
