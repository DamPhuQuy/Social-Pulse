package com.socialpulse.app.bookmark.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.bookmark.adapter.persistence.BookmarkRepositoryAdapter;
import com.socialpulse.app.bookmark.application.service.BookmarkResponseMapper;
import com.socialpulse.app.bookmark.application.service.CreateBookmarkService;
import com.socialpulse.app.bookmark.application.service.DeleteBookmarkService;
import com.socialpulse.app.bookmark.application.service.GetBookmarksService;
import com.socialpulse.app.bookmark.application.usecase.CreateBookmarkUseCase;
import com.socialpulse.app.bookmark.application.usecase.DeleteBookmarkUseCase;
import com.socialpulse.app.bookmark.application.usecase.GetBookmarksUseCase;
import com.socialpulse.app.bookmark.domain.repository.BookmarkRepository;
import com.socialpulse.app.bookmark.infrastructure.persistence.mapper.BookmarkPersistenceMapper;
import com.socialpulse.app.bookmark.infrastructure.persistence.repository.JpaBookmarkRepository;
import com.socialpulse.app.post.application.service.PostSummaryAssembler;
import com.socialpulse.app.post.domain.repository.PostRepository;

@Configuration
public class BookmarkConfig {
    @Bean
    public BookmarkRepository bookmarkRepository(
            JpaBookmarkRepository jpaBookmarkRepository,
            BookmarkPersistenceMapper bookmarkPersistenceMapper) {
        return new BookmarkRepositoryAdapter(jpaBookmarkRepository, bookmarkPersistenceMapper);
    }

    @Bean
    public CreateBookmarkUseCase createBookmarkUseCase(
            BookmarkRepository bookmarkRepository,
            PostRepository postRepository,
            BookmarkResponseMapper bookmarkResponseMapper) {
        return new CreateBookmarkService(bookmarkRepository, postRepository, bookmarkResponseMapper);
    }

    @Bean
    public DeleteBookmarkUseCase deleteBookmarkUseCase(BookmarkRepository bookmarkRepository) {
        return new DeleteBookmarkService(bookmarkRepository);
    }

    @Bean
    public GetBookmarksUseCase getBookmarksUseCase(
            BookmarkRepository bookmarkRepository,
            PostRepository postRepository,
            PostSummaryAssembler postSummaryAssembler) {
        return new GetBookmarksService(bookmarkRepository, postRepository, postSummaryAssembler);
    }
}
