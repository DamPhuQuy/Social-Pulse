package com.socialpulse.app.common.cloudinary.adapter.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.socialpulse.app.common.cloudinary.service.CloudinaryService;
import com.socialpulse.app.common.dto.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/media")
@Tag(name = "Media", description = "Media upload APIs")
public class MediaController {

    private final CloudinaryService cloudinaryService;

    public MediaController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload media file", description = "Uploads an image or video file to Cloudinary and returns the URL")
    public ResponseEntity<ApiResponse<String>> uploadMedia(@RequestParam("file") MultipartFile file) {
        String url = cloudinaryService.upload(file);
        return ResponseEntity.ok(
            ApiResponse.<String>builder()
                .data(url)
                .message("Media uploaded successfully")
                .build()
        );
    }
}
