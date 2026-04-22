package com.socialpulse.app.post.application.dto.request;

import com.socialpulse.app.post.domain.enums.Privacy;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class PostCreationRequest {
    @NotBlank(message = "Content must not be blank")
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;

    @NotBlank(message = "Image URL must not be blank")
    @Size(max = 2048, message = "Image URL must not exceed 2048 characters")
    private String imageUrl;

    @NotBlank(message = "Image public ID must not be blank")
    @Size(max = 255, message = "Image public ID must not exceed 255 characters")
    private String imagePublicId;

    @NotNull(message = "Privacy setting must not be null")
    private Privacy privacy;

    @Nullable
    private Long parentPostId;
}
