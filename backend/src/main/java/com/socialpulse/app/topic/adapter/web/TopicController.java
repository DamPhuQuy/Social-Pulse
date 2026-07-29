package com.socialpulse.app.topic.adapter.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.security.permission.RequiresPermission;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.topic.adapter.web.dto.TopicRequest;
import com.socialpulse.app.topic.adapter.web.dto.TopicResponse;
import com.socialpulse.app.topic.application.service.TopicService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
@Tag(name = "Topics", description = "Topic management and follow APIs")
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    @Operation(summary = "Get all topics")
    public ResponseEntity<List<TopicResponse>> getAllTopics() {
        return ResponseEntity.ok(topicService.getAllTopics());
    }

    @GetMapping("/followed")
    @Operation(summary = "Get topic slugs followed by current user")
    public ResponseEntity<ApiResponse<List<String>>> getFollowedTopics(@AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            return ResponseEntity.ok(ApiResponse.<List<String>>builder().data(List.of()).build());
        }
        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .data(topicService.getFollowedTopicSlugs(currentUser.getId()))
                .build());
    }

    @PostMapping("/{slug}/follow")
    @Operation(summary = "Follow a topic")
    public ResponseEntity<ApiResponse<Void>> followTopic(
            @PathVariable String slug,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser != null) {
            topicService.followTopic(currentUser.getId(), slug);
        }
        return ResponseEntity.ok(ApiResponse.<Void>builder().message("Followed topic " + slug).build());
    }

    @DeleteMapping("/{slug}/follow")
    @Operation(summary = "Unfollow a topic")
    public ResponseEntity<ApiResponse<Void>> unfollowTopic(
            @PathVariable String slug,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser != null) {
            topicService.unfollowTopic(currentUser.getId(), slug);
        }
        return ResponseEntity.ok(ApiResponse.<Void>builder().message("Unfollowed topic " + slug).build());
    }

    @PostMapping
    @RequiresPermission.TopicManage
    @Operation(summary = "Create a topic (admin)")
    public ResponseEntity<ApiResponse<TopicResponse>> createTopic(@RequestBody @Valid TopicRequest request) {
        return ResponseEntity.ok(ApiResponse.<TopicResponse>builder()
                .data(topicService.createTopic(request))
                .message("Topic created")
                .build());
    }

    @PutMapping("/{id}")
    @RequiresPermission.TopicManage
    @Operation(summary = "Update a topic (admin)")
    public ResponseEntity<ApiResponse<TopicResponse>> updateTopic(
            @PathVariable Long id,
            @RequestBody @Valid TopicRequest request) {
        return ResponseEntity.ok(ApiResponse.<TopicResponse>builder()
                .data(topicService.updateTopic(id, request))
                .message("Topic updated")
                .build());
    }

    @DeleteMapping("/{id}")
    @RequiresPermission.TopicManage
    @Operation(summary = "Delete a topic (admin)")
    public ResponseEntity<ApiResponse<Void>> deleteTopic(@PathVariable Long id) {
        topicService.deleteTopic(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().message("Topic deleted").build());
    }
}
