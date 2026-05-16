package com.socialpulse.app.notification.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;

import com.socialpulse.app.notification.domain.model.Notification;
import com.socialpulse.app.notification.infrastructure.persistence.entity.NotificationEntity;

@Mapper(componentModel = "spring")
public interface NotificationPersistenceMapper {
    Notification toDomain(NotificationEntity entity);

    NotificationEntity toEntity(Notification domain);
}
