package com.socialpulse.app.post.application.service;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;
import com.socialpulse.app.post.application.usecase.GetUserPostsUseCase;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class GetUserPostsService implements GetUserPostsUseCase {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostSummaryAssembler postSummaryAssembler;

    public GetUserPostsService(
            PostRepository postRepository,
            UserRepository userRepository,
            PostSummaryAssembler postSummaryAssembler) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postSummaryAssembler = postSummaryAssembler;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserPostResponse> getUserPosts(Long userId, int page, int size, CustomUserDetails currentUser) {
        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean canViewPrivate = userId.equals(currentUser.getId()) || hasPostManagePermission(currentUser);

        Page<Post> posts = canViewPrivate
                ? postRepository.findActiveByUserId(userId, pageable)
                : postRepository.findActiveByUserIdAndPrivacy(userId, Privacy.PUBLIC, pageable);

        return PageResponse.<UserPostResponse>builder()
                .items(postSummaryAssembler.assemble(posts.getContent(), currentUser.getId()))
                .page(posts.getNumber())
                .size(posts.getSize())
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .hasNext(posts.hasNext())
                .build();
    }

    private boolean hasPostManagePermission(CustomUserDetails currentUser) {
        return currentUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("post:manage"));
    }
}
