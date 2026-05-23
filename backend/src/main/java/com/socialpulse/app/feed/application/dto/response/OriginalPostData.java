package com.socialpulse.app.feed.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Embedded snapshot of the original (parent) post for SHARE-type feed items.
 * Allows the frontend to render the quoted original post without a second API call.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OriginalPostData {
    private Long postId;
    private String content;
    private String imageUrl;
    private List<String> topicSlugs;
    private Long userId;
    private String username;
    private String userAvatar;
    private LocalDateTime createdAt;
}
