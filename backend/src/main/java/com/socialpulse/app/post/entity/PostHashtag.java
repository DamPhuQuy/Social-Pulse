package com.socialpulse.app.post.entity;

import java.io.Serializable;

import com.socialpulse.app.common.entity.Hashtag;

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
@Table(name = "post_hashtags", indexes = {
    @Index(name = "idx_post_hashtags_post_id", columnList = "post_id"),
    @Index(name = "idx_post_hashtags_hashtag_id", columnList = "hashtag_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(PostHashtag.PostHashtagId.class)
public class PostHashtag {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id", nullable = false)
    private Hashtag hashtag;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostHashtagId implements Serializable {
        private Long post;
        private Long hashtag;
    }
}
