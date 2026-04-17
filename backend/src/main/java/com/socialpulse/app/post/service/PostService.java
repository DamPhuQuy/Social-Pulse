package com.socialpulse.app.post.service;

import org.springframework.stereotype.Service;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.status.ErrorCode;
import com.socialpulse.app.post.dto.request.PostCreationRequest;
import com.socialpulse.app.post.dto.response.PostCreationResponse;
import com.socialpulse.app.post.dto.response.ViewPostResponse;
import com.socialpulse.app.post.dto.request.ViewPostRequest;
import com.socialpulse.app.post.entity.Post;
import com.socialpulse.app.post.entity.Privacy;
import com.socialpulse.app.post.repository.PostRepository;
import com.socialpulse.app.user.entity.User;
import com.socialpulse.app.user.repository.UserRepository;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository,
                       UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    public PostCreationResponse createPost(PostCreationRequest request, Long userId) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.builder()
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .imagePublicId(request.getImagePublicId())
                .privacy(request.getPrivacy() == null ? Privacy.PUBLIC : request.getPrivacy())
                .user(currentUser)
                .build();

        Post savedPost = postRepository.save(post);

        return PostCreationResponse.builder()
                .id(savedPost.getId())
                .userId(savedPost.getUser().getId())
                .content(savedPost.getContent())
                .imageUrl((savedPost.getImageUrl()))
                .imagePublicId(savedPost.getImagePublicId())
                .createdAt(savedPost.getCreatedAt())
                .build();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ViewPostResponse viewPost(ViewPostRequest request){
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        return ViewPostResponse.builder()
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .imagePublicId(post.getImagePublicId())
                .privacy(post.getPrivacy())
                .userId(post.getUser().getId())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
