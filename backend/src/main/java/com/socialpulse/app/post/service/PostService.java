package com.socialpulse.app.post.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.status.PostCode;
import com.socialpulse.app.common.status.UserCode;
import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.post.dto.request.PostCreationRequest;
import com.socialpulse.app.post.dto.request.PostReactionRequest;
import com.socialpulse.app.post.dto.response.PostCreationResponse;
import com.socialpulse.app.post.dto.response.PostReactionResponse;
import com.socialpulse.app.post.dto.response.ViewPostResponse;
import com.socialpulse.app.post.entity.Post;
import com.socialpulse.app.post.entity.PostReactions;
import com.socialpulse.app.post.entity.Privacy;
import com.socialpulse.app.post.mapper.PostMapper;
import com.socialpulse.app.post.mapper.PostReactionMapper;
import com.socialpulse.app.post.repository.PostReactionRepository;
import com.socialpulse.app.post.repository.PostRepository;
import com.socialpulse.app.user.entity.User;
import com.socialpulse.app.user.repository.UserRepository;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostReactionRepository postReactionRepository;
    private final PostMapper postMapper;
    private final PostReactionMapper postReactionMapper;

    public PostService(PostRepository postRepository,
                       UserRepository userRepository,
                       PostReactionRepository postReactionRepository,
                       PostMapper postMapper,
                       PostReactionMapper postReactionMapper) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postReactionRepository = postReactionRepository;
        this.postMapper = postMapper;
        this.postReactionMapper = postReactionMapper;
    }

    public PostCreationResponse createPost(PostCreationRequest request, CustomUserDetails currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        Post post = postMapper.toPost(request, user);

        Post savedPost = postRepository.save(post);

        return postMapper.toPostCreationResponse(savedPost);
    }

    @Transactional(readOnly = true)
    public ViewPostResponse viewPost(Long postId, CustomUserDetails currentUser){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

        Long userId = currentUser.getId();

        // post is not public and the current user is not the owner of the post
        if (!post.getPrivacy().equals(Privacy.PUBLIC) && !post.getUser().getId().equals(userId)) {
            throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
        }

        return postMapper.toViewPostResponse(post);
    }

    @Transactional
    public PostReactionResponse react(PostReactionRequest request, CustomUserDetails currentUser) {
        Post post = postRepository.findById(request.getPostId())
            .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

        User user = userRepository.findById(currentUser.getId())
            .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        PostReactions currentReaction = postReactionRepository.findByPostIdAndUserId(request.getPostId(), currentUser.getId())
                .orElse(null);

        ReactionType targetReaction = request.getReactionType();

        if (currentReaction == null) {
            PostReactions newReaction = postReactionMapper.toPostReaction(user, post, targetReaction);

            PostReactions savedReaction = postReactionRepository.save(newReaction);
            incrementReactionCount(post, targetReaction);
            postRepository.save(post);

            return postReactionMapper.toPostReactionResponse(savedReaction);
        }

        // remove the reaction if the user clicks the same reaction again
        if (currentReaction.getReactionType() == targetReaction) {
            postReactionRepository.delete(currentReaction);
            decrementReactionCount(post, targetReaction);
            postRepository.save(post);
            return postReactionMapper.toPostReactionResponse(currentReaction);
        }

        // user changes reaction if current reaction != target reaction
        decrementReactionCount(post, currentReaction.getReactionType());
        incrementReactionCount(post, targetReaction);
        currentReaction.setReactionType(targetReaction);

        PostReactions updatedReaction = postReactionRepository.save(currentReaction);
        postRepository.save(post);

        return postReactionMapper.toPostReactionResponse(updatedReaction);
    }

    private void incrementReactionCount(Post post, ReactionType reactionType) {
        if (reactionType == ReactionType.UPVOTE) {
            post.setUpvoteCount(safeCount(post.getUpvoteCount()) + 1L);
            return;
        }

        if (reactionType == ReactionType.DOWNVOTE) {
            post.setDownvoteCount(safeCount(post.getDownvoteCount()) + 1L);
        }
    }

    private void decrementReactionCount(Post post, ReactionType reactionType) {
        if (reactionType == ReactionType.UPVOTE) {
            long currentUpvotes = safeCount(post.getUpvoteCount());
            post.setUpvoteCount(Math.max(0L, currentUpvotes - 1L));
            return;
        }

        if (reactionType == ReactionType.DOWNVOTE) {
            long currentDownvotes = safeCount(post.getDownvoteCount());
            post.setDownvoteCount(Math.max(0L, currentDownvotes - 1L));
        }
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }

}
