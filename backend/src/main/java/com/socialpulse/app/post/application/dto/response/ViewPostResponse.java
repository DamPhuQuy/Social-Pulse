package com.socialpulse.app.post.application.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViewPostResponse {
    private Long id;

    private String content;
    private String imageUrl;

    private Long userId;
    private String username;
    private String userAvatar;

    private int upvoteCount;
    private int downvoteCount;

    private int cmtCount;
    private int shareCount;

    private Integer myVote;   // +1, -1, 0

    private LocalDateTime createdAt;
}
