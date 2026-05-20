package com.socialpulse.app.comment.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.model.User;

@ExtendWith(MockitoExtension.class)
class DeleteCommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;

    private DeleteCommentService service;

    @BeforeEach
    void setUp() {
        service = new DeleteCommentService(commentRepository, postRepository);
    }

    private CustomUserDetails userDetails(Long id) {
        User user = User.builder().id(id).email(id + "@mail.com").username("user" + id).roles(Set.of()).build();
        return new CustomUserDetails(user);
    }

    @Test
    void deleteComment_asOwner_succeeds() {
        Comment comment = Comment.builder().id(1L).postId(10L).userId(5L).build();
        Post post = Post.builder().id(10L).userId(5L).privacy(Privacy.PUBLIC).cmtCount(3L).build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        service.deleteComment(10L, 1L, userDetails(5L));

        verify(commentRepository).save(comment);
        verify(postRepository).save(post);
    }

    @Test
    void deleteComment_notFound_throws() {
        when(commentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> service.deleteComment(10L, 1L, userDetails(5L)));
    }

    @Test
    void deleteComment_wrongPost_throws() {
        Comment comment = Comment.builder().id(1L).postId(99L).userId(5L).build();
        Post post = Post.builder().id(10L).userId(5L).privacy(Privacy.PUBLIC).cmtCount(3L).build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThrows(AppException.class, () -> service.deleteComment(10L, 1L, userDetails(5L)));
    }

    @Test
    void deleteComment_notOwnerNoPermission_throws() {
        Comment comment = Comment.builder().id(1L).postId(10L).userId(5L).build();
        Post post = Post.builder().id(10L).userId(5L).privacy(Privacy.PUBLIC).cmtCount(3L).build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThrows(AppException.class, () -> service.deleteComment(10L, 1L, userDetails(99L)));
    }
}
