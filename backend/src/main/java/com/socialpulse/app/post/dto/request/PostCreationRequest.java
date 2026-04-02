package com.socialpulse.app.post.dto.request;

import com.socialpulse.app.post.entity.Privacy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class PostCreationRequest {
    private String content;
    private String imageUrl;
    private Privacy privacy;
}
