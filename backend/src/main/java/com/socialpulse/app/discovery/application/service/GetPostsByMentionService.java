package com.socialpulse.app.discovery.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.discovery.application.usecase.GetPostsByMentionUseCase;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;
import com.socialpulse.app.post.application.service.PostSummaryAssembler;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;

public class GetPostsByMentionService implements GetPostsByMentionUseCase {
    private final PostRepository postRepository;
    private final PostSummaryAssembler postSummaryAssembler;

    public GetPostsByMentionService(PostRepository postRepository, PostSummaryAssembler postSummaryAssembler) {
        this.postRepository = postRepository;
        this.postSummaryAssembler = postSummaryAssembler;
    }

    @Override
    public PageResponse<UserPostResponse> getPostsByMention(String username, int page, int size) {
        String normalizedMention = username.startsWith("@") ? username.toLowerCase() : "@" + username.toLowerCase();
        Page<Post> posts = postRepository.findPublicActiveByMention(
                normalizedMention,
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
}
