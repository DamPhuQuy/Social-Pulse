package com.socialpulse.app.notification.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.notification.adapter.persistence.NotificationRepositoryAdapter;
import com.socialpulse.app.notification.application.service.GetNotificationsService;
import com.socialpulse.app.notification.application.service.GetUnreadNotificationCountService;
import com.socialpulse.app.notification.application.service.MarkAllNotificationsReadService;
import com.socialpulse.app.notification.application.service.MarkNotificationReadService;
import com.socialpulse.app.notification.application.service.NotificationResponseAssembler;
import com.socialpulse.app.notification.application.usecase.GetNotificationsUseCase;
import com.socialpulse.app.notification.application.usecase.GetUnreadNotificationCountUseCase;
import com.socialpulse.app.notification.application.usecase.MarkAllNotificationsReadUseCase;
import com.socialpulse.app.notification.application.usecase.MarkNotificationReadUseCase;
import com.socialpulse.app.notification.domain.repository.NotificationRepository;
import com.socialpulse.app.notification.infrastructure.persistence.mapper.NotificationPersistenceMapper;
import com.socialpulse.app.notification.infrastructure.persistence.repository.JpaNotificationRepository;

@Configuration
public class NotificationConfig {
    @Bean
    public NotificationRepository notificationRepository(
            JpaNotificationRepository jpaNotificationRepository,
            NotificationPersistenceMapper notificationPersistenceMapper) {
        return new NotificationRepositoryAdapter(jpaNotificationRepository, notificationPersistenceMapper);
    }

    @Bean
    public GetNotificationsUseCase getNotificationsUseCase(
            NotificationRepository notificationRepository,
            NotificationResponseAssembler notificationResponseAssembler) {
        return new GetNotificationsService(notificationRepository, notificationResponseAssembler);
    }

    @Bean
    public GetUnreadNotificationCountUseCase getUnreadNotificationCountUseCase(
            NotificationRepository notificationRepository) {
        return new GetUnreadNotificationCountService(notificationRepository);
    }

    @Bean
    public MarkNotificationReadUseCase markNotificationReadUseCase(
            NotificationRepository notificationRepository,
            NotificationResponseAssembler notificationResponseAssembler) {
        return new MarkNotificationReadService(notificationRepository, notificationResponseAssembler);
    }

    @Bean
    public MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase(
            NotificationRepository notificationRepository) {
        return new MarkAllNotificationsReadService(notificationRepository);
    }
}
