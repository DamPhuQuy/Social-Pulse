package com.socialpulse.app.discovery.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.socialpulse.app.discovery.domain.model.SearchHistory;
import com.socialpulse.app.discovery.infrastructure.persistence.entity.SearchHistoryEntity;

@Component
public class SearchHistoryPersistenceMapper {

    public SearchHistory toDomain(SearchHistoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return SearchHistory.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .keyword(entity.getKeyword())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SearchHistoryEntity toEntity(SearchHistory domain) {
        if (domain == null) {
            return null;
        }

        return SearchHistoryEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .keyword(domain.getKeyword())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
