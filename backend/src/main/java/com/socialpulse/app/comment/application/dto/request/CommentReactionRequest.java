package com.socialpulse.app.comment.application.dto.request;

import com.socialpulse.app.common.utils.ReactionType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CommentReactionRequest {
    @NotNull(message = "Reaction type must not be null")
    private ReactionType reactionType;
}
