package com.socialpulse.app.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CommentCreationRequest {
    @NotBlank(message = "Content must not be blank")
    private String content;

    @NotNull(message = "Post ID must not be null")
    private Long postId;

    private Long parentCommentId;// optional
}
