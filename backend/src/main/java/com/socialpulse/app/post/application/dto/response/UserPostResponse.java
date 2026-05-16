package com.socialpulse.app.post.application.dto.response;

import java.time.LocalDateTime;

import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.enums.Privacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPostResponse {
    private Long id;
    private Long parentPostId;
    private PostType type;
    private String content;
    private String imageUrl;
    private Long userId;
    private String username;
    private String userAvatar;
    private Privacy privacy;
    private long upvoteCount;
    private long downvoteCount;
    private long cmtCount;
    private long shareCount;
    private LocalDateTime createdAt;
}
