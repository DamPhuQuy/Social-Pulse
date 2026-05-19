package com.socialpulse.app.post.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostTopicResponse {
    private String slug;
    private String label;
    private String category;
}
