package com.socialpulse.app.share.application.dto.request;

import com.socialpulse.app.post.domain.enums.Privacy;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ShareCreationRequest {
    @NotNull(message = "Post id must not be null")
    private Long postId;

    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;

    private Privacy privacy;
}
