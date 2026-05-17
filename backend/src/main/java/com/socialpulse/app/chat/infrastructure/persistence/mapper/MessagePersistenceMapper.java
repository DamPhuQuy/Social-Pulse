package com.socialpulse.app.chat.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;

import com.socialpulse.app.chat.domain.model.Message;
import com.socialpulse.app.chat.infrastructure.persistence.entity.MessageEntity;

@Mapper(componentModel = "spring")
public interface MessagePersistenceMapper {
    Message toDomain(MessageEntity entity);

    MessageEntity toEntity(Message domain);
}
