package com.socialpulse.app.chat.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;

import com.socialpulse.app.chat.domain.model.Conversation;
import com.socialpulse.app.chat.infrastructure.persistence.entity.ConversationEntity;

@Mapper(componentModel = "spring")
public interface ConversationPersistenceMapper {
    Conversation toDomain(ConversationEntity entity);

    ConversationEntity toEntity(Conversation domain);
}
