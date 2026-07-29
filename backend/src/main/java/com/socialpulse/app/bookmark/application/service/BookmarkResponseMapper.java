package com.socialpulse.app.bookmark.application.service;
import org.springframework.stereotype.Component;

import org.springframework.stereotype.Service;

import com.socialpulse.app.bookmark.application.dto.response.BookmarkResponse;
import com.socialpulse.app.bookmark.domain.model.Bookmark;

@Service
@Component
public class BookmarkResponseMapper {
    public BookmarkResponse toResponse(Bookmark bookmark) {
        return BookmarkResponse.builder()
                .id(bookmark.getId())
                .userId(bookmark.getUserId())
                .postId(bookmark.getPostId())
                .createdAt(bookmark.getCreatedAt())
                .build();
    }
}
