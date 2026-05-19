package com.socialpulse.app.post.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.enums.Privacy;

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
    private Long parentPostId;
    private PostType type;

    private String content;
    private String imageUrl;
    private List<String> topicSlugs;
    private Privacy privacy;

    private Long userId;
    private String username;
    private String userAvatar;

    private int upvoteCount;
    private int downvoteCount;

    private int cmtCount;
    private int shareCount;

    private Integer myVote;   // +1, -1, 0

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
