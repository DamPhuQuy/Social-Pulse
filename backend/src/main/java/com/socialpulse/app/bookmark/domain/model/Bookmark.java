package com.socialpulse.app.bookmark.domain.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bookmark {
    private Long id;
    private Long userId;
    private Long postId;
    private LocalDateTime createdAt;
}
