package com.socialpulse.app.behavior.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.behavior.adapter.persistence.UserBehaviorRepositoryAdapter;
import com.socialpulse.app.behavior.application.service.BehaviorFeaturesExtractionService;
import com.socialpulse.app.behavior.application.service.BehaviorTrackingService;
import com.socialpulse.app.behavior.application.usecase.BehaviorFeaturesExtractionUseCase;
import com.socialpulse.app.behavior.application.usecase.BehaviorTrackingUseCase;
import com.socialpulse.app.behavior.domain.repository.UserBehaviorRepository;
import com.socialpulse.app.behavior.infrastructure.persistence.repository.UserBehaviorJpaRepository;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;

@Configuration
public class BehaviorConfig {
    @Bean
    public UserBehaviorRepository userBehavior(UserBehaviorJpaRepository jpaRepository) {
        return new UserBehaviorRepositoryAdapter(jpaRepository);
    }

    @Bean
    public BehaviorTrackingUseCase trackBehaviorUseCase(UserBehaviorRepository repository) {
        return new BehaviorTrackingService(repository);
    }

    @Bean
    public BehaviorFeaturesExtractionUseCase behaviorFeaturesExtractionUseCase(
            UserBehaviorRepository repository,
            FollowRepository followRepository,
            PostRepository postRepository) {
        return new BehaviorFeaturesExtractionService(repository, followRepository, postRepository);
    }
}

