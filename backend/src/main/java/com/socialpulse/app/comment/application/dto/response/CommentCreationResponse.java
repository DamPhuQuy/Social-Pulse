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
public class CommentCreationResponse {
    private Long id;
    private Long postId;
    private UserSummary user;
    private Long parentCommentId;
    private String content;
    private LocalDateTime createdAt;
    private boolean edited;
    private int replyCount;
}
