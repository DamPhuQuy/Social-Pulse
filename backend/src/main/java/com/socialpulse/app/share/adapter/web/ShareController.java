package com.socialpulse.app.share.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.share.application.dto.request.ShareCreationRequest;
import com.socialpulse.app.share.application.dto.response.ShareCreationResponse;
import com.socialpulse.app.share.application.usecase.ShareUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/shares")
@Tag(name = "Shares", description = "Share post APIs")
public class ShareController {

    private final ShareUseCase shareUseCase;

    public ShareController(ShareUseCase shareUseCase) {
        this.shareUseCase = shareUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') and hasAuthority('CREATE_POST')")
    @Operation(summary = "Share post", description = "Create a share-post from an existing post")
    public ResponseEntity<ApiResponse<ShareCreationResponse>> createShare(
            @RequestBody @Valid ShareCreationRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        ShareCreationResponse response = shareUseCase.createShare(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ShareCreationResponse>builder().data(response).build());
    }
}
