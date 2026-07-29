package com.socialpulse.app.comment.application.service;
import org.springframework.stereotype.Service;

import java.util.List;

import com.socialpulse.app.comment.application.dto.response.CommentResponse;
import com.socialpulse.app.comment.application.usecase.GetTopLevelCommentsUseCase;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;

@Service
public class GetTopLevelCommentsService implements GetTopLevelCommentsUseCase {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentResponseAssembler commentResponseAssembler;

    public GetTopLevelCommentsService(CommentRepository commentRepository,
                                      PostRepository postRepository,
                                      CommentResponseAssembler commentResponseAssembler) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.commentResponseAssembler = commentResponseAssembler;
    }

    @Override
    public List<CommentResponse> getTopLevelComments(Long postId, Long lastId, int limit, Long viewerUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));
        validatePostAccessible(post, viewerUserId);

        List<Comment> comments = commentRepository.findTopLevelCommentsByPostId(postId, lastId == null ? 0L : lastId, limit);
        
        if (comments.isEmpty()) {
            return List.of();
        }

        return commentResponseAssembler.toCommentResponses(comments, viewerUserId);
    }

    private void validatePostAccessible(Post post, Long viewerUserId) {
        if (post.getDeletedAt() != null) {
            throw new AppException(PostCode.POST_NOT_FOUND);
        }

        if (!post.isPublic() && !post.getUserId().equals(viewerUserId)) {
            throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
        }
    }
}
