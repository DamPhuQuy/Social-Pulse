package com.socialpulse.app.share.application.usecase;

import com.socialpulse.app.share.application.dto.request.ShareCreationRequest;
import com.socialpulse.app.share.application.dto.response.ShareCreationResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface ShareUseCase {
    ShareCreationResponse createShare(ShareCreationRequest request, CustomUserDetails currentUser);
}
