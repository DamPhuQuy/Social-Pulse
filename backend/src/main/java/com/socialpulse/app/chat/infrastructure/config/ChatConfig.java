package com.socialpulse.app.chat.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.chat.adapter.persistence.ConversationRepositoryAdapter;
import com.socialpulse.app.chat.adapter.persistence.MessageRepositoryAdapter;
import com.socialpulse.app.chat.domain.repository.ConversationRepository;
import com.socialpulse.app.chat.domain.repository.MessageRepository;
import com.socialpulse.app.chat.infrastructure.persistence.mapper.ConversationPersistenceMapper;
import com.socialpulse.app.chat.infrastructure.persistence.mapper.MessagePersistenceMapper;
import com.socialpulse.app.chat.infrastructure.persistence.repository.JpaConversationRepository;
import com.socialpulse.app.chat.infrastructure.persistence.repository.JpaMessageRepository;

@Configuration
public class ChatConfig {

    // adapters --------------------------------------

    @Bean
    public ConversationRepository conversationRepository(
            JpaConversationRepository jpaConversationRepository,
            ConversationPersistenceMapper conversationPersistenceMapper) {
        return new ConversationRepositoryAdapter(jpaConversationRepository, conversationPersistenceMapper);
    }

    @Bean
    public MessageRepository messageRepository(
            JpaMessageRepository jpaMessageRepository,
            MessagePersistenceMapper messagePersistenceMapper) {
        return new MessageRepositoryAdapter(jpaMessageRepository, messagePersistenceMapper);
    }
}
