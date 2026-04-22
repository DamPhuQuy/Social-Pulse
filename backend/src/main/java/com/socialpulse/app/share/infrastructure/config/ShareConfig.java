package com.socialpulse.app.share.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.share.adapter.persistence.ShareRepositoryAdapter;
import com.socialpulse.app.share.domain.repository.ShareRepository;
import com.socialpulse.app.share.infrastructure.persistence.repository.JpaShareRepository;

@Configuration
public class ShareConfig {
    @Bean
    public ShareRepository shareRepository(JpaShareRepository jpaShareRepository) {
        return new ShareRepositoryAdapter(jpaShareRepository);
    }
}
