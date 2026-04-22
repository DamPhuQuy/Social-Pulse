package com.socialpulse.app.share.domain.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Share {
    private Long id;
    private Long postId;
    private Long userId;
    private LocalDateTime createdAt;
}
