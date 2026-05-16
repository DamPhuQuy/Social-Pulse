# Quy trình triển khai tính năng Soft Delete Comment

## Mô tả chức năng
Cho phép user xóa comment của chính họ. Không xóa dữ liệu thật khỏi database mà sử dụng soft delete với flag `deleted = true`.

## API Design
- **Endpoint**: `DELETE /api/v1/posts/{postId}/comments/{commentId}`
- **Authorization**: Chỉ owner của comment mới được delete
- **Method**: Soft delete (set `deleted = true`)

---

## Thứ tự triển khai code

### Bước 1: Kiểm tra Entity (CommentEntity.java)
**File**: `backend/src/main/java/com/socialpulse/app/comment/infrastructure/persistence/entity/CommentEntity.java`

**Mục đích**: Đảm bảo có trường `deleted` trong database entity

**Nội dung**: 
- Trường `deleted` đã tồn tại ở line 74
- Không cần thay đổi gì

```java
@Column(nullable = false)
@Builder.Default
private boolean deleted = false;
```

---

### Bước 2: Kiểm tra Domain Model (Comment.java)
**File**: `backend/src/main/java/com/socialpulse/app/comment/domain/model/Comment.java`

**Mục đích**: Đảm bảo domain model có trường `deleted` và method `markDeleted()`

**Nội dung**:
- Trường `deleted` đã tồn tại ở line 23
- Method `markDeleted()` đã tồn tại ở line 42-44
- Không cần thay đổi gì

```java
private boolean deleted;

public void markDeleted() {
    this.deleted = true;
}
```

---

### Bước 3: Tạo Use Case Interface (DeleteCommentUseCase.java)
**File**: `backend/src/main/java/com/socialpulse/app/comment/application/usecase/DeleteCommentUseCase.java`

**Mục đích**: Định nghĩa contract cho chức năng delete comment

**Nội dung**:
```java
package com.socialpulse.app.comment.application.usecase;

import com.socialpulse.app.security.user.CustomUserDetails;

public interface DeleteCommentUseCase {
    void deleteComment(Long postId, Long commentId, CustomUserDetails currentUser);
}
```

**Giải thích**:
- Interface định nghĩa method `deleteComment` với 3 tham số:
  - `postId`: ID của post chứa comment
  - `commentId`: ID của comment cần xóa
  - `currentUser`: Thông tin user hiện tại (để kiểm tra ownership)

---

### Bước 4: Tạo Service Implementation (DeleteCommentService.java)
**File**: `backend/src/main/java/com/socialpulse/app/comment/application/service/DeleteCommentService.java`

**Mục đích**: Implement business logic cho việc soft delete comment

**Nội dung**:
```java
package com.socialpulse.app.comment.application.service;

import com.socialpulse.app.comment.application.usecase.DeleteCommentUseCase;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.CommentCode;
import com.socialpulse.app.security.user.CustomUserDetails;

public class DeleteCommentService implements DeleteCommentUseCase {

    private final CommentRepository commentRepository;

    public DeleteCommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Override
    public void deleteComment(Long postId, Long commentId, CustomUserDetails currentUser) {
        // 1. Tìm comment theo ID
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(CommentCode.COMMENT_NOT_FOUND));

        // 2. Kiểm tra comment có thuộc post này không
        if (!comment.getPostId().equals(postId)) {
            throw new AppException(CommentCode.COMMENT_NOT_BELONG_TO_POST);
        }

        // 3. Kiểm tra user có phải owner của comment không
        if (!comment.getUserId().equals(currentUser.getId())) {
            throw new AppException(CommentCode.COMMENT_NOT_OWNER);
        }

        // 4. Kiểm tra comment đã bị xóa chưa
        if (comment.isDeleted()) {
            throw new AppException(CommentCode.COMMENT_ALREADY_DELETED);
        }

        // 5. Soft delete: set deleted = true
        comment.markDeleted();
        commentRepository.save(comment);
    }
}
```

**Giải thích logic**:
1. Tìm comment trong database
2. Validate comment thuộc đúng post
3. Validate user là owner của comment
4. Kiểm tra comment chưa bị xóa trước đó
5. Gọi `markDeleted()` để set `deleted = true` và lưu vào database

---

### Bước 5: Đăng ký Bean trong Config (CommentConfig.java)
**File**: `backend/src/main/java/com/socialpulse/app/comment/infrastructure/config/CommentConfig.java`

**Mục đích**: Đăng ký service bean để Spring có thể inject dependency

**Thay đổi**:

1. Thêm import:
```java
import com.socialpulse.app.comment.application.usecase.DeleteCommentUseCase;
import com.socialpulse.app.comment.application.service.DeleteCommentService;
```

2. Thêm bean method:
```java
@Bean
public DeleteCommentUseCase deleteCommentUseCase(CommentRepository commentRepositoryPort) {
    return new DeleteCommentService(commentRepositoryPort);
}
```

**Giải thích**:
- Spring sẽ tự động inject `CommentRepository` vào constructor của `DeleteCommentService`
- Bean này có thể được inject vào Controller

---

### Bước 6: Thêm DELETE Endpoint vào Controller (CommentController.java)
**File**: `backend/src/main/java/com/socialpulse/app/comment/adapter/web/CommentController.java`

**Mục đích**: Expose REST API endpoint cho client

**Thay đổi**:

1. Thêm import:
```java
import org.springframework.web.bind.annotation.DeleteMapping;
import com.socialpulse.app.comment.application.usecase.DeleteCommentUseCase;
```

2. Thêm field và constructor parameter:
```java
private final DeleteCommentUseCase deleteCommentUseCase;

public CommentController(CreateCommentUseCase createCommentUseCase,
                       UpdateCommentUseCase updateCommentUseCase,
                       DeleteCommentUseCase deleteCommentUseCase,
                       GetTopLevelCommentsUseCase getTopLevelCommentsUseCase) {
    this.createCommentUseCase = createCommentUseCase;
    this.updateCommentUseCase = updateCommentUseCase;
    this.deleteCommentUseCase = deleteCommentUseCase;
    this.getTopLevelCommentsUseCase = getTopLevelCommentsUseCase;
}
```

3. Thêm DELETE endpoint method:
```java
@DeleteMapping("/{commentId}")
@PreAuthorize("hasAuthority('comment:delete')")
@Operation(
    summary = "Delete comment",
    description = "Soft delete a comment (only owner can delete)",
    responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Comment deleted successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Not the owner of the comment"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Comment not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    }
)
public ResponseEntity<ApiResponse<Void>> deleteComment(
    @PathVariable Long postId,
    @PathVariable Long commentId,
    @AuthenticationPrincipal CustomUserDetails currentUser) {

    deleteCommentUseCase.deleteComment(postId, commentId, currentUser);

    return ResponseEntity.ok(ApiResponse.<Void>builder()
            .code(200)
            .message("Comment deleted successfully.")
            .build());
}
```

**Giải thích**:
- `@DeleteMapping("/{commentId}")`: Map HTTP DELETE request
- `@PreAuthorize("hasAuthority('comment:delete')")`: Kiểm tra quyền delete
- `@PathVariable`: Lấy postId và commentId từ URL
- `@AuthenticationPrincipal`: Lấy thông tin user đang login
- Return `ApiResponse<Void>` vì không cần trả về data

---

## Tóm tắt thứ tự file

1. ✅ **CommentEntity.java** - Kiểm tra trường `deleted` (đã có sẵn)
2. ✅ **Comment.java** - Kiểm tra method `markDeleted()` (đã có sẵn)
3. ✅ **DeleteCommentUseCase.java** - Tạo interface mới
4. ✅ **DeleteCommentService.java** - Tạo service implementation mới
5. ✅ **CommentConfig.java** - Đăng ký bean
6. ✅ **CommentController.java** - Thêm DELETE endpoint

---

## Kiểm tra Exception Codes

Cần đảm bảo các exception code sau tồn tại trong `CommentCode.java`:
- `COMMENT_NOT_FOUND`
- `COMMENT_NOT_BELONG_TO_POST`
- `COMMENT_NOT_OWNER`
- `COMMENT_ALREADY_DELETED` (có thể cần thêm mới)

---

## Testing

### Test case cần kiểm tra:
1. ✅ User xóa comment của chính họ → Success
2. ❌ User xóa comment của người khác → 403 Forbidden
3. ❌ Xóa comment không tồn tại → 404 Not Found
4. ❌ Xóa comment đã bị xóa trước đó → 400 Bad Request
5. ❌ Comment không thuộc post được chỉ định → 400 Bad Request
6. ❌ User chưa login → 401 Unauthorized

### Cách test:
```bash
# Test xóa comment thành công
curl -X DELETE http://localhost:8080/api/v1/posts/1/comments/123 \
  -H "Authorization: Bearer YOUR_TOKEN"

# Kiểm tra database: deleted = true, nhưng record vẫn còn
```

---

## Lưu ý quan trọng

1. **Soft Delete**: Không xóa record khỏi database, chỉ set `deleted = true`
2. **Ownership Check**: Chỉ owner mới được xóa comment của mình
3. **Security**: Sử dụng `@PreAuthorize` để kiểm tra quyền
4. **Idempotent**: Không cho phép xóa comment đã bị xóa (throw exception)
5. **Clean Architecture**: Tuân thủ kiến trúc hexagonal (domain → application → adapter)

---

## Ngày triển khai
2026-05-14
