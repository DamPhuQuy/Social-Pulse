package com.socialpulse.app.topic.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.topic.adapter.web.dto.TopicRequest;
import com.socialpulse.app.topic.adapter.web.dto.TopicResponse;
import com.socialpulse.app.topic.infrastructure.persistence.entity.TopicEntity;
import com.socialpulse.app.topic.infrastructure.persistence.entity.TopicFollowEntity;
import com.socialpulse.app.topic.infrastructure.persistence.repository.JpaTopicFollowRepository;
import com.socialpulse.app.topic.infrastructure.persistence.repository.TopicRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final JpaTopicFollowRepository topicFollowRepository;

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

    @Transactional
    public void followTopic(Long userId, String topicSlug) {
        String normalizedSlug = topicSlug.trim().toLowerCase();
        if (!topicFollowRepository.existsByUserIdAndTopicSlug(userId, normalizedSlug)) {
            TopicFollowEntity entity = TopicFollowEntity.builder()
                    .userId(userId)
                    .topicSlug(normalizedSlug)
                    .createdAt(LocalDateTime.now())
                    .build();
            topicFollowRepository.save(entity);
        }
    }

    @Transactional
    public void unfollowTopic(Long userId, String topicSlug) {
        String normalizedSlug = topicSlug.trim().toLowerCase();
        topicFollowRepository.deleteByUserIdAndTopicSlug(userId, normalizedSlug);
    }

    @Transactional(readOnly = true)
    public List<String> getFollowedTopicSlugs(Long userId) {
        return topicFollowRepository.findFollowedTopicSlugsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean isFollowingTopic(Long userId, String topicSlug) {
        if (userId == null) return false;
        return topicFollowRepository.existsByUserIdAndTopicSlug(userId, topicSlug.trim().toLowerCase());
    }

    private TopicResponse toResponse(TopicEntity e) {
        return TopicResponse.builder().id(e.getId()).name(e.getName()).slug(e.getSlug()).build();
    }
}
