package com.socialpulse.app.feed.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.enums.Privacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedItemResponse {
    private Long postId;
    private Long parentPostId;
    private PostType type;
    private String content;
    private String imageUrl;
    private List<String> topicSlugs;
    private Long userId;
    private String username;
    private String userAvatar;
    private long upvoteCount;
    private long downvoteCount;
    private long cmtCount;
    private long shareCount;
    private ReactionType myReaction;
    private Integer myVote;
    private Double aiScore;
    private String source;
    private String rankingProvider;
    private String featureSchemaVersion;
    private LocalDateTime rankedAt;
    private Privacy privacy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private OriginalPostData originalPost;
}
