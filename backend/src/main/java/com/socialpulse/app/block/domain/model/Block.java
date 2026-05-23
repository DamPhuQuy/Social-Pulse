package com.socialpulse.app.block.domain.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Block {
    private Long id;
    private Long blockerId;
    private Long blockedId;
    private LocalDateTime createdAt;
}
