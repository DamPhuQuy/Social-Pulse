package com.socialpulse.app.user.entity;

import java.io.Serializable;

import com.socialpulse.app.common.entity.Topic;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_topics", indexes = {
    @Index(name = "idx_user_topics_user_id", columnList = "user_id"),
    @Index(name = "idx_user_topics_topic_id", columnList = "topic_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserTopic.UserTopicId.class)
public class UserTopic {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserTopicId implements Serializable {
        private Long user;
        private Long topic;
    }
}
