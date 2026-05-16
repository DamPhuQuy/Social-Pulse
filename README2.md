# Cấu trúc thư mục Comment Module - Hexagonal Architecture

## Tổng quan
Module Comment được xây dựng theo kiến trúc Hexagonal (Ports & Adapters), tuân thủ nguyên tắc Clean Architecture với sự phân tách rõ ràng giữa các layer.

---

## Cấu trúc thư mục

```
comment/
├── adapter/                    # Adapter Layer (Infrastructure)
│   ├── persistence/           # Database adapter
│   └── web/                   # REST API adapter
├── application/               # Application Layer
│   ├── dto/                   # Data Transfer Objects
│   │   ├── mapper/           # DTO mappers
│   │   ├── request/          # Request DTOs
│   │   └── response/         # Response DTOs
│   ├── service/              # Service implementations
│   └── usecase/              # Use case interfaces
├── domain/                    # Domain Layer (Core Business Logic)
│   ├── model/                # Domain models
│   └── repository/           # Repository interfaces (ports)
└── infrastructure/            # Infrastructure Layer
    ├── config/               # Spring configuration
    └── persistence/          # JPA implementation
        ├── entity/           # JPA entities
        ├── mapper/           # Entity-Domain mappers
        └── repository/       # JPA repositories
```

---

## Chi tiết từng folder

### 1. `adapter/` - Adapter Layer
**Mục đích**: Chứa các adapter kết nối ứng dụng với thế giới bên ngoài (external systems)

#### 1.1. `adapter/persistence/` - Database Adapter
**Chức năng**: Implement repository interfaces từ domain layer để tương tác với database

**Files**:
- `CommentRepositoryAdapter.java`: Adapter chuyển đổi giữa domain repository interface và JPA repository

**Nhiệm vụ**:
- Implement `CommentRepository` interface từ domain
- Gọi `JpaCommentRepository` để thực hiện database operations
- Sử dụng `CommentPersistenceMapper` để convert giữa Entity và Domain model
- Đóng vai trò là cầu nối giữa domain logic và database

**Ví dụ**:
```java
@Override
public Optional<Comment> findById(Long id) {
    return jpaCommentRepository.findById(id)
            .map(commentPersistenceMapper::toDomain);
}
```

#### 1.2. `adapter/web/` - REST API Adapter
**Chức năng**: Expose REST API endpoints cho client

**Files**:
- `CommentController.java`: REST controller xử lý HTTP requests

**Nhiệm vụ**:
- Nhận HTTP requests từ client
- Validate input data với `@Valid`
- Gọi use case để xử lý business logic
- Trả về HTTP response với format chuẩn
- Xử lý authentication/authorization với `@PreAuthorize`

**Endpoints**:
- `POST /api/v1/posts/{postId}/comments` - Tạo comment
- `GET /api/v1/posts/{postId}/comments` - Lấy danh sách comments
- `PUT /api/v1/posts/{postId}/comments/{commentId}` - Cập nhật comment
- `DELETE /api/v1/posts/{postId}/comments/{commentId}` - Xóa comment (soft delete)

---

### 2. `application/` - Application Layer
**Mục đích**: Chứa business logic và orchestration của các use cases

#### 2.1. `application/dto/` - Data Transfer Objects
**Chức năng**: Định nghĩa các object dùng để transfer data giữa layers

##### 2.1.1. `dto/mapper/`
**Files**: `CommentMapper.java`

**Chức năng**:
- Convert giữa Domain model và DTO
- Mapping `Comment` → `CommentResponse`
- Mapping `Comment` → `CommentCreationResponse`

**Ví dụ**:
```java
public CommentResponse toCommentResponse(Comment comment, User user) {
    return CommentResponse.builder()
        .id(comment.getId())
        .content(comment.getContent())
        .userId(user.getId())
        .username(user.getUsername())
        .build();
}
```

##### 2.1.2. `dto/request/`
**Files**:
- `CommentCreationRequest.java`: DTO cho tạo comment mới
- `CommentUpdateRequest.java`: DTO cho cập nhật comment

**Chức năng**:
- Nhận data từ client
- Validate input với Bean Validation annotations (`@NotBlank`, `@Size`, etc.)

**Ví dụ**:
```java
public class CommentCreationRequest {
    @NotBlank(message = "Content is required")
    @Size(max = 10000)
    private String content;
    
    private Long parentCommentId;
}
```

##### 2.1.3. `dto/response/`
**Files**:
- `CommentResponse.java`: DTO trả về thông tin comment
- `CommentCreationResponse.java`: DTO trả về sau khi tạo/update comment

**Chức năng**:
- Format data trả về cho client
- Chỉ expose những field cần thiết (không expose sensitive data)

#### 2.2. `application/service/` - Service Implementations
**Chức năng**: Implement các use case interfaces, chứa business logic chính

**Files**:
- `CreateCommentService.java`: Logic tạo comment
- `UpdateCommentService.java`: Logic cập nhật comment
- `DeleteCommentService.java`: Logic xóa comment (soft delete)
- `GetTopLevelCommentsService.java`: Logic lấy danh sách comments
- `ValidateParentCommentService.java`: Logic validate parent comment

**Nhiệm vụ**:
- Implement business rules và validation
- Orchestrate các domain objects
- Gọi repository để persist data
- Xử lý exceptions và error cases

**Ví dụ flow trong DeleteCommentService**:
1. Tìm comment theo ID
2. Validate comment thuộc đúng post
3. Validate user là owner
4. Kiểm tra comment chưa bị xóa
5. Gọi `comment.markDeleted()` (domain logic)
6. Save vào database

#### 2.3. `application/usecase/` - Use Case Interfaces
**Chức năng**: Định nghĩa contracts (interfaces) cho các use cases

**Files**:
- `CreateCommentUseCase.java`
- `UpdateCommentUseCase.java`
- `DeleteCommentUseCase.java`
- `GetTopLevelCommentsUseCase.java`
- `ValidateParentCommentUseCase.java`

**Nhiệm vụ**:
- Định nghĩa public API của application layer
- Cho phép dependency inversion (depend on abstraction, not implementation)
- Dễ dàng test và mock

**Ví dụ**:
```java
public interface DeleteCommentUseCase {
    void deleteComment(Long postId, Long commentId, CustomUserDetails currentUser);
}
```

---

### 3. `domain/` - Domain Layer (Core)
**Mục đích**: Chứa business logic thuần túy, không phụ thuộc vào framework hay infrastructure

#### 3.1. `domain/model/` - Domain Models
**Files**:
- `Comment.java`: Domain model của Comment
- `CommentReaction.java`: Domain model của Comment Reaction

**Chức năng**:
- Chứa business logic thuần túy
- Encapsulate business rules
- Không phụ thuộc vào database, framework, hay external systems

**Ví dụ business logic trong Comment.java**:
```java
public void markDeleted() {
    this.deleted = true;
}

public void updateContent(String newContent) {
    this.content = newContent;
    this.edited = true;
}

public void incrementUpvoteCount() {
    this.upvoteCount = safeCount(this.upvoteCount) + 1L;
}
```

**Đặc điểm**:
- Pure Java objects (POJOs)
- Không có annotations của JPA hay Spring
- Chứa business invariants và rules

#### 3.2. `domain/repository/` - Repository Interfaces (Ports)
**Files**: `CommentRepository.java`

**Chức năng**:
- Định nghĩa contract để persist/retrieve domain objects
- Port trong Hexagonal Architecture
- Được implement bởi adapter layer

**Ví dụ**:
```java
public interface CommentRepository {
    Optional<Comment> findById(Long id);
    Comment save(Comment comment);
    List<Comment> findTopLevelCommentsByPostId(Long postId, long lastId, int limit);
}
```

**Đặc điểm**:
- Chỉ làm việc với domain models, không biết về entities
- Không phụ thuộc vào JPA hay database cụ thể

---

### 4. `infrastructure/` - Infrastructure Layer
**Mục đích**: Chứa các implementation details về technical concerns

#### 4.1. `infrastructure/config/` - Spring Configuration
**Files**: `CommentConfig.java`

**Chức năng**:
- Đăng ký Spring beans
- Wire dependencies giữa các layers
- Dependency Injection configuration

**Ví dụ**:
```java
@Bean
public DeleteCommentUseCase deleteCommentUseCase(CommentRepository commentRepositoryPort) {
    return new DeleteCommentService(commentRepositoryPort);
}

@Bean
public CommentRepository commentRepositoryPort(JpaCommentRepository jpaCommentRepository,
                                CommentPersistenceMapper commentPersistenceMapper) {
    return new CommentRepositoryAdapter(jpaCommentRepository, commentPersistenceMapper);
}
```

#### 4.2. `infrastructure/persistence/` - JPA Implementation
**Chức năng**: Chứa tất cả code liên quan đến database persistence

##### 4.2.1. `persistence/entity/`
**Files**:
- `CommentEntity.java`: JPA entity mapping với database table
- `CommentReactionEntity.java`: JPA entity cho comment reactions

**Chức năng**:
- Map với database tables
- Chứa JPA annotations (`@Entity`, `@Table`, `@Column`, etc.)
- Định nghĩa relationships (`@ManyToOne`, `@OneToMany`)
- Định nghĩa indexes cho performance

**Ví dụ**:
```java
@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comment_post", columnList = "post_id"),
    @Index(name = "idx_comment_user", columnList = "user_id")
})
public class CommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
```

##### 4.2.2. `persistence/mapper/`
**Files**: `CommentPersistenceMapper.java`

**Chức năng**:
- Convert giữa JPA Entity và Domain Model
- `CommentEntity` ↔ `Comment`
- Tách biệt domain model khỏi database concerns

**Ví dụ**:
```java
public Comment toDomain(CommentEntity entity) {
    return Comment.builder()
        .id(entity.getId())
        .postId(entity.getPost().getId())
        .userId(entity.getUser().getId())
        .content(entity.getContent())
        .deleted(entity.isDeleted())
        .build();
}

public CommentEntity toEntity(Comment comment) {
    // Convert domain model to entity
}
```

##### 4.2.3. `persistence/repository/`
**Files**: `JpaCommentRepository.java`

**Chức năng**:
- Spring Data JPA repository interface
- Định nghĩa custom queries với `@Query`
- Extend `JpaRepository` để có sẵn CRUD operations

**Ví dụ**:
```java
public interface JpaCommentRepository extends JpaRepository<CommentEntity, Long> {
    @Query("SELECT c FROM CommentEntity c WHERE c.post.id = :postId " +
           "AND c.parentComment IS NULL AND c.id > :lastId " +
           "ORDER BY c.id ASC")
    List<CommentEntity> findTopLevelCommentsByPostId(
        @Param("postId") Long postId,
        @Param("lastId") Long lastId,
        Pageable pageable
    );
}
```

---

## Luồng dữ liệu (Data Flow)

### Request Flow (Client → Database)
```
1. Client
   ↓ HTTP Request
2. CommentController (adapter/web)
   ↓ Call use case
3. DeleteCommentService (application/service)
   ↓ Business logic + validation
4. Comment Domain Model (domain/model)
   ↓ markDeleted()
5. CommentRepository Interface (domain/repository)
   ↓ save(comment)
6. CommentRepositoryAdapter (adapter/persistence)
   ↓ Convert to entity
7. CommentPersistenceMapper (infrastructure/persistence/mapper)
   ↓ toEntity()
8. JpaCommentRepository (infrastructure/persistence/repository)
   ↓ JPA save
9. Database
```

### Response Flow (Database → Client)
```
1. Database
   ↓ JPA query
2. JpaCommentRepository
   ↓ Return CommentEntity
3. CommentPersistenceMapper
   ↓ toDomain()
4. Comment Domain Model
   ↓ Return to service
5. DeleteCommentService
   ↓ Return to controller
6. CommentController
   ↓ HTTP Response
7. Client
```

---

## Nguyên tắc thiết kế

### 1. Dependency Rule
- Dependencies chỉ đi từ ngoài vào trong
- Domain layer không phụ thuộc vào bất kỳ layer nào
- Application layer chỉ phụ thuộc vào domain
- Adapter/Infrastructure phụ thuộc vào application và domain

```
Infrastructure → Application → Domain
     ↑
  Adapter
```

### 2. Separation of Concerns
- **Domain**: Business logic thuần túy
- **Application**: Use case orchestration
- **Adapter**: External system integration
- **Infrastructure**: Technical implementation details

### 3. Testability
- Domain models dễ test (pure Java, no dependencies)
- Use cases có thể test với mock repositories
- Adapters có thể test riêng biệt

### 4. Flexibility
- Dễ dàng thay đổi database (chỉ cần implement repository interface mới)
- Dễ dàng thay đổi API format (chỉ cần thay đổi controller)
- Business logic không bị ảnh hưởng bởi infrastructure changes

---

## Ví dụ: Delete Comment Flow

### 1. Client gửi request
```http
DELETE /api/v1/posts/1/comments/123
Authorization: Bearer token
```

### 2. CommentController nhận request
```java
@DeleteMapping("/{commentId}")
public ResponseEntity<ApiResponse<Void>> deleteComment(
    @PathVariable Long postId,
    @PathVariable Long commentId,
    @AuthenticationPrincipal CustomUserDetails currentUser) {
    
    deleteCommentUseCase.deleteComment(postId, commentId, currentUser);
    return ResponseEntity.ok(...);
}
```

### 3. DeleteCommentService xử lý business logic
```java
public void deleteComment(Long postId, Long commentId, CustomUserDetails currentUser) {
    // 1. Find comment
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new AppException(CommentCode.COMMENT_NOT_FOUND));
    
    // 2. Validate ownership
    if (!comment.getUserId().equals(currentUser.getId())) {
        throw new AppException(CommentCode.COMMENT_NOT_OWNER);
    }
    
    // 3. Soft delete
    comment.markDeleted();
    commentRepository.save(comment);
}
```

### 4. Comment domain model thực hiện business logic
```java
public void markDeleted() {
    this.deleted = true;
}
```

### 5. CommentRepositoryAdapter convert và save
```java
public Comment save(Comment comment) {
    return commentPersistenceMapper.toDomain(
        jpaCommentRepository.save(commentPersistenceMapper.toEntity(comment))
    );
}
```

### 6. Database được update
```sql
UPDATE comments SET deleted = true WHERE id = 123;
```

---

## Lợi ích của kiến trúc này

### 1. Maintainability
- Code được tổ chức rõ ràng theo chức năng
- Dễ tìm và sửa bugs
- Dễ thêm features mới

### 2. Testability
- Mỗi layer có thể test độc lập
- Domain logic test không cần database
- Use cases test với mock repositories

### 3. Flexibility
- Thay đổi database không ảnh hưởng business logic
- Thay đổi API format không ảnh hưởng domain
- Dễ dàng migrate sang technology stack khác

### 4. Scalability
- Có thể tách các layer thành microservices
- Có thể scale từng layer độc lập
- Dễ dàng thêm caching, message queue, etc.

### 5. Team Collaboration
- Nhiều developers có thể làm việc song song trên các layer khác nhau
- Clear boundaries giữa các responsibilities
- Ít conflict khi merge code

---

## Best Practices

### 1. Domain Layer
- Giữ domain models pure (không có framework dependencies)
- Business logic phải ở trong domain models
- Sử dụng value objects cho concepts quan trọng

### 2. Application Layer
- Services chỉ orchestrate, không chứa business logic
- Validate input ở DTO level
- Handle exceptions và convert sang application-specific errors

### 3. Adapter Layer
- Controllers chỉ handle HTTP concerns
- Repository adapters chỉ convert giữa domain và persistence

### 4. Infrastructure Layer
- Giữ JPA entities đơn giản (chỉ mapping)
- Configuration phải rõ ràng và dễ hiểu
- Sử dụng indexes cho performance

---

## Tổng kết

Cấu trúc folder của Comment module tuân thủ Hexagonal Architecture với:
- **Domain** ở trung tâm (business logic thuần túy)
- **Application** bao quanh domain (use cases)
- **Adapters** kết nối với external systems (web, database)
- **Infrastructure** cung cấp technical implementations

Kiến trúc này đảm bảo code dễ maintain, test, và mở rộng trong tương lai.

---

**Ngày tạo**: 2026-05-14
