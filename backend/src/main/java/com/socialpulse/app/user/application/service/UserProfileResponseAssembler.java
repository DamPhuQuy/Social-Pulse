package com.socialpulse.app.user.application.service;

import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.application.dto.response.UserViewProfileResponse;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.model.UserProfile;

public class UserProfileResponseAssembler {

    private final PostRepository postRepository;
    private final FollowRepository followRepository;

    public UserProfileResponseAssembler(PostRepository postRepository,
                                        FollowRepository followRepository) {
        this.postRepository = postRepository;
        this.followRepository = followRepository;
    }

    public UserViewProfileResponse assemble(User user, UserProfile userProfile, Long viewerUserId) {
        long postCount = postRepository.countByUserId(user.getId());
        long followers = followRepository.countByFollowingId(user.getId());
        long following = followRepository.countByFollowerId(user.getId());

        boolean isFollowing = viewerUserId != null
                && !viewerUserId.equals(user.getId())
                && followRepository.existsByFollowerIdAndFollowingId(viewerUserId, user.getId());

        return UserViewProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(userProfile.getDisplayName())
                .bio(userProfile.getBio())
                .avatarUrl(userProfile.getAvatarUrl())
                .dob(userProfile.getDob())
                .gender(userProfile.getGender())
                .postCount(postCount)
                .followers(followers)
                .following(following)
                .isFollowing(isFollowing)
                .build();
    }
}
