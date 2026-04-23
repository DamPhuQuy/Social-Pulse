package com.socialpulse.app.feed.domain.model;

import com.socialpulse.app.feed.domain.enums.Source;
import com.socialpulse.app.post.domain.model.Post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatePost {
    private Post post;
    private Source source;
}
