package com.socialpulse.app.topic.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.socialpulse.app.topic.adapter.web.dto.TopicRequest;
import com.socialpulse.app.topic.adapter.web.dto.TopicResponse;
import com.socialpulse.app.topic.infrastructure.persistence.entity.TopicEntity;
import com.socialpulse.app.topic.infrastructure.persistence.repository.TopicRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    public List<TopicResponse> getAllTopics() {
        return topicRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TopicResponse createTopic(TopicRequest request) {
        TopicEntity entity = TopicEntity.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return toResponse(topicRepository.save(entity));
    }

    public TopicResponse updateTopic(Long id, TopicRequest request) {
        TopicEntity entity = topicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Topic not found: " + id));
        entity.setName(request.getName());
        entity.setSlug(request.getSlug());
        entity.setUpdatedAt(LocalDateTime.now());
        return toResponse(topicRepository.save(entity));
    }

    public void deleteTopic(Long id) {
        if (!topicRepository.existsById(id)) {
            throw new EntityNotFoundException("Topic not found: " + id);
        }
        topicRepository.deleteById(id);
    }

    private TopicResponse toResponse(TopicEntity e) {
        return TopicResponse.builder().id(e.getId()).name(e.getName()).slug(e.getSlug()).build();
    }
}
