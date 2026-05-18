package com.socialpulse.app.topic.adapter.web;

import java.util.List;

import com.socialpulse.app.security.permission.RequiresPermission;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.common.dto.response.ApiResponse;
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
@Tag(name = "Topics", description = "Topic APIs")
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    @Operation(summary = "Get all topics")
    public ResponseEntity<List<TopicResponse>> getAllTopics() {
        return ResponseEntity.ok(topicService.getAllTopics());
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
