package com.socialpulse.app.discovery.domain.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistory {
    private Long id;
    private Long userId;
    private String keyword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void updateSearchTime() {
        this.updatedAt = LocalDateTime.now();
    }
}
