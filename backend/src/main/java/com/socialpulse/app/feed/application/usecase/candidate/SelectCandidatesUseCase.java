package com.socialpulse.app.feed.application.usecase.candidate;

import java.util.List;

import com.socialpulse.app.feed.domain.model.CandidatePost;

public interface SelectCandidatesUseCase {
    List<CandidatePost> selectCandidates(Long userId);
}
