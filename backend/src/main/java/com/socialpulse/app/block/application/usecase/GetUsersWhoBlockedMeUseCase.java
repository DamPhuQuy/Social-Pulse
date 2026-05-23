package com.socialpulse.app.block.application.usecase;

import java.util.List;

public interface GetUsersWhoBlockedMeUseCase {
    List<Long> getUsersWhoBlockedMe(Long blockedId);
}
