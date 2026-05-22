package com.socialpulse.app.block.application.usecase;

import java.util.List;

public interface GetBlockedUserIdsUseCase {
    List<Long> getBlockedUserIds(Long blockerId);
}
