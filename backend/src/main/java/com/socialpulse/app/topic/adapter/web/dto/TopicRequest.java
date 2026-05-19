package com.socialpulse.app.topic.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TopicRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String slug;
}
