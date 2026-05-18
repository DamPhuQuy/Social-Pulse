package com.socialpulse.app.discovery.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.socialpulse.app.post.application.service.PostSummaryAssembler;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;

@ExtendWith(MockitoExtension.class)
class SearchPostsServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private PostSummaryAssembler postSummaryAssembler;

    private SearchPostsService service;

    @BeforeEach
    void setUp() {
        service = new SearchPostsService(postRepository, postSummaryAssembler);
    }

    @Test
    void searchPosts_callsRepository() {
        Page<Post> page = new PageImpl<>(List.of(Post.builder().id(1L).build()));
        when(postRepository.searchPublicActiveByContent(eq("hello"), any())).thenReturn(page);

        var result = service.searchPosts("hello", 0, 10);

        assertNotNull(result);
        verify(postRepository).searchPublicActiveByContent(eq("hello"), any());
    }

    @Test
    void searchPosts_blankQuery_returnsEmpty() {
        var result = service.searchPosts("", 0, 10);

        assertNotNull(result);
        verifyNoInteractions(postRepository);
    }

    @Test
    void searchPosts_nullQuery_returnsEmpty() {
        var result = service.searchPosts(null, 0, 10);

        assertNotNull(result);
        verifyNoInteractions(postRepository);
    }
}
