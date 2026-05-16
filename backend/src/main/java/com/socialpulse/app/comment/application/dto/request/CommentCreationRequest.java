package com.socialpulse.app.comment.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    @Size(max = 2000, message = "Content must not exceed 2000 characters")
    private String content;

    @Positive(message = "Parent comment ID must be greater than 0")
    private Long parentCommentId; // optional
}
