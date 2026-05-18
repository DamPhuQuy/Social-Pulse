package com.socialpulse.app.discovery.application.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistoryResponse {
    private Long id;
    private String keyword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
