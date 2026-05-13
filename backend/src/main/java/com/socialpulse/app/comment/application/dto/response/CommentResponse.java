package com.socialpulse.app.comment.application.dto.response;

import java.time.LocalDateTime;

import com.socialpulse.app.user.application.dto.response.UserSummary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CommentResponse {
    private Long id;
    private UserSummary user;
    private String content;
    private LocalDateTime createdAt;
    private Long upvoteCount;
    private Long downvoteCount;
    private int replyCount;
}
