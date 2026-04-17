package com.socialpulse.app.post.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor @AllArgsConstructor
@Getter
public class PostReactionRequest {
    @NotNull(message = "Post ID must not be null")
    @Positive(message = "Post ID must be greater than 0")
    private Long postId;
}
