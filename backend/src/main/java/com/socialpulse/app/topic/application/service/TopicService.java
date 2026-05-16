package com.socialpulse.app.topic.application.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.socialpulse.app.topic.adapter.web.dto.TopicResponse;
import com.socialpulse.app.topic.infrastructure.persistence.repository.TopicRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    public List<TopicResponse> getAllTopics() {
        return topicRepository.findAllByOrderByNameAsc().stream()
                .map(entity -> TopicResponse.builder()
                        .id(entity.getId())
                        .name(entity.getName())
                        .slug(entity.getSlug())
                        .build())
                .collect(Collectors.toList());
    }
}
