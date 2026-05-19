package com.socialpulse.app.topic.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.socialpulse.app.topic.domain.model.Topic;
import com.socialpulse.app.topic.infrastructure.persistence.entity.TopicEntity;

@Component
public class TopicPersistenceMapper {

    public Topic toDomain(TopicEntity entity) {
        if (entity == null) {
            return null;
        }
        return Topic.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public TopicEntity toEntity(Topic domain) {
        if (domain == null) {
            return null;
        }
        return TopicEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .slug(domain.getSlug())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
