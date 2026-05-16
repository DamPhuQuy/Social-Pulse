package com.socialpulse.app.post.application.dto.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.socialpulse.app.post.application.dto.response.ViewPostResponse;
import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.model.UserProfile;

class PostMapperTest {
    private final PostMapper postMapper = Mappers.getMapper(PostMapper.class);

    @Test
    void mapsAuthorAndVoteIntoViewPostResponse() {
        Post post = Post.builder()
                .id(10L)
                .userId(5L)
                .parentPostId(null)
                .type(PostType.ORIGINAL)
                .content("hello")
                .imageUrl("https://cdn.example.com/post.png")
                .privacy(Privacy.PUBLIC)
                .upvoteCount(12L)
                .downvoteCount(2L)
                .cmtCount(4L)
                .shareCount(3L)
                .createdAt(LocalDateTime.of(2026, 5, 16, 10, 30))
                .build();
        User author = User.builder()
                .id(5L)
                .username("alice")
                .profile(UserProfile.builder().avatarUrl("https://cdn.example.com/avatar.png").build())
                .build();

        ViewPostResponse response = postMapper.toViewPostResponse(post, author, 1);

        assertEquals(10L, response.getId());
        assertEquals(5L, response.getUserId());
        assertEquals("alice", response.getUsername());
        assertEquals("https://cdn.example.com/avatar.png", response.getUserAvatar());
        assertEquals(1, response.getMyVote());
        assertEquals("hello", response.getContent());
        assertEquals(12, response.getUpvoteCount());
        assertEquals(2, response.getDownvoteCount());
        assertEquals(4, response.getCmtCount());
        assertEquals(3, response.getShareCount());
    }
}
