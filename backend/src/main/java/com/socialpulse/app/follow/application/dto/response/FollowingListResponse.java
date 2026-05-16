package com.socialpulse.app.follow.application.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowingListResponse {
    private List<FollowingResponse> following;
    private Long totalCount;
    private Integer page;
    private Integer size;
    private Boolean hasNext;
}
