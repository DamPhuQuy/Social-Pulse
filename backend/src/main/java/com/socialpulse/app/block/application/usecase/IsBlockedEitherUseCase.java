package com.socialpulse.app.block.application.usecase;

public interface IsBlockedEitherUseCase {
    boolean isBlockedEither(Long userA, Long userB);
}
