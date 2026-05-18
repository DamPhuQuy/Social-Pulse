package com.socialpulse.app.user.application.service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.topic.infrastructure.persistence.mapper.TopicPersistenceMapper;
import com.socialpulse.app.topic.infrastructure.persistence.repository.TopicRepository;
import com.socialpulse.app.user.application.dto.request.UpdateUserTopicsRequest;
import com.socialpulse.app.user.application.usecase.UpdateUserTopicsUseCase;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class UpdateUserTopicsService implements UpdateUserTopicsUseCase {

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final TopicPersistenceMapper topicMapper;

    public UpdateUserTopicsService(UserRepository userRepository, TopicRepository topicRepository, TopicPersistenceMapper topicMapper) {
        this.userRepository = userRepository;
        this.topicRepository = topicRepository;
        this.topicMapper = topicMapper;
    }

    @Override
    @Transactional
    public void updateTopics(Long userId, UpdateUserTopicsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        Set<com.socialpulse.app.topic.domain.model.Topic> selectedTopics = new HashSet<>();
        
        if (request.getTopicIds() != null && !request.getTopicIds().isEmpty()) {
            selectedTopics = topicRepository.findAllById(request.getTopicIds()).stream()
                    .map(topicMapper::toDomain)
                    .collect(Collectors.toSet());
        }

        user.updateTopics(selectedTopics);
        userRepository.save(user);
    }
}
