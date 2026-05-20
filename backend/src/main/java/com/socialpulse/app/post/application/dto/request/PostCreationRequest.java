package com.socialpulse.app.post.application.dto.request;

import java.util.List;

import com.socialpulse.app.post.domain.enums.Privacy;

import jakarta.validation.constraints.NotEmpty;
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

    @Size(max = 2048, message = "Image URL must not exceed 2048 characters")
    private String imageUrl;

    @Size(max = 255, message = "Image public ID must not exceed 255 characters")
    private String imagePublicId;

    @NotEmpty(message = "At least one topic must be selected")
    @Size(max = 5, message = "A post can have at most 5 topics")
    private List<@NotBlank(message = "Topic must not be blank") @Size(max = 80, message = "Topic must not exceed 80 characters") String> topicSlugs;

    @NotNull(message = "Privacy setting must not be null")
    private Privacy privacy;

    private Long parentPostId;

    private Long topicId;
}

