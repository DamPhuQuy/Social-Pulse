package com.socialpulse.app.discovery.application.service;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;
import com.socialpulse.app.post.application.service.PostSummaryAssembler;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.discovery.application.usecase.SearchPostsUseCase;

@Service
public class SearchPostsService implements SearchPostsUseCase {
    private final PostRepository postRepository;
    private final PostSummaryAssembler postSummaryAssembler;

    public SearchPostsService(PostRepository postRepository, PostSummaryAssembler postSummaryAssembler) {
        this.postRepository = postRepository;
        this.postSummaryAssembler = postSummaryAssembler;
    }

    @Override
    public PageResponse<UserPostResponse> searchPosts(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return emptyPage(page, size);
        }

        Page<Post> posts = postRepository.searchPublicActiveByContent(
                query.trim(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return PageResponse.<UserPostResponse>builder()
                .items(postSummaryAssembler.assemble(posts.getContent()))
                .page(posts.getNumber())
                .size(posts.getSize())
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .hasNext(posts.hasNext())
                .build();
    }

    private PageResponse<UserPostResponse> emptyPage(int page, int size) {
        return PageResponse.<UserPostResponse>builder()
                .items(java.util.List.of())
                .page(page)
                .size(size)
                .totalElements(0)
                .totalPages(0)
                .hasNext(false)
                .build();
    }
}
