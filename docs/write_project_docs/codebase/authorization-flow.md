# Authorization Flow - Social Pulse

## Tổng Quan

Social Pulse sử dụng hệ thống authorization dựa trên **database-driven roles và permissions** với **scope-based naming** (ví dụ: `post:create`, `user:manage`). Hệ thống này cho phép quản lý quyền linh hoạt mà không cần sửa code.

## Kiến Trúc Authorization

### 1. Database Schema

```sql
-- Bảng permissions: lưu các quyền cụ thể
permissions (id, name, description, created_at, updated_at)
  - Ví dụ: post:create, post:delete:any, user:manage

-- Bảng roles: lưu các vai trò
roles (id, name, description, created_at, updated_at)
  - Ví dụ: GUEST, USER, ADMIN

-- Bảng role_permissions: liên kết role với permissions
role_permissions (role_id, permission_id)

-- Bảng user_roles: liên kết user với roles
user_roles (user_id, role_id)
```

### 2. Domain Models

**Permission Domain Model** (`user/domain/model/Permission.java`):
```java
public class Permission {
    private Long id;
    private String name;           // Scope format: resource:action
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Role Domain Model** (`user/domain/model/Role.java`):
```java
public class Role {
    private Long id;
    private String name;           // GUEST, USER, ADMIN
    private String description;
    private Set<Permission> permissions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Helper method để kiểm tra permission
    public boolean hasPermission(String permission) {
        return permissions.stream()
            .anyMatch(p -> p.getName().equals(permission));
    }
}
```

**User Domain Model** (`user/domain/model/User.java`):
```java
public class User {
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private Set<Role> roles;       // User có thể có nhiều roles
    // ... other fields
}
```

## Luồng Authorization Chi Tiết

### Phase 1: User Login & Load Authorities

#### 1.1. CustomUserDetailsService Load User

**File**: `auth/application/service/user/CustomUserDetailsService.java`

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        // Load user từ database với EAGER fetch roles và permissions
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Wrap user trong CustomUserDetails
        return CustomUserDetails.builder()
                .user(user)
                .build();
    }
}
```

**Điều gì xảy ra:**
- Spring Security gọi `loadUserByUsername()` khi user login
- UserRepository load User từ database
- Do `UserEntity` có `@ManyToMany(fetch = FetchType.EAGER)` với roles, nên roles và permissions được load cùng lúc
- Trả về `CustomUserDetails` chứa User với đầy đủ roles và permissions

#### 1.2. CustomUserDetails Convert Roles → Authorities

**File**: `security/user/CustomUserDetails.java`

```java
@Builder
public class CustomUserDetails implements UserDetails {
    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Duyệt qua tất cả roles của user
        user.getRoles().forEach(role -> {
            // Thêm role authority với prefix ROLE_
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

            // Thêm tất cả permissions của role đó
            role.getPermissions().forEach(permission ->
                authorities.add(new SimpleGrantedAuthority(permission.getName()))
            );
        });

        return authorities;
    }
    
    // ... other UserDetails methods
}
```

**Ví dụ Output:**

User có role `ADMIN` sẽ có authorities:
```
[
  "ROLE_ADMIN",
  "post:read",
  "post:create", 
  "post:update",
  "post:delete",
  "post:delete:any",      // Chỉ ADMIN mới có
  "comment:create",
  "comment:delete:any",   // Chỉ ADMIN mới có
  "user:read",
  "user:manage",          // Chỉ ADMIN mới có
  "user:moderate"         // Chỉ ADMIN mới có
]
```

User có role `USER` sẽ có authorities:
```
[
  "ROLE_USER",
  "post:read",
  "post:create",
  "post:update",
  "post:delete",          // Chỉ delete post của mình
  "comment:create",
  "comment:update",
  "comment:delete",       // Chỉ delete comment của mình
  "user:read",
  "user:update"           // Chỉ update profile của mình
]
```

### Phase 2: Generate JWT Token

**File**: `auth/application/service/jwt/JwtService.java`

```java
@Override
public String generateToken(CustomUserDetails userDetails) {
    Map<String, Object> extraClaims = new HashMap<>();

    // Thêm userId vào JWT claims
    extraClaims.put("userId", userDetails.getId());
    
    // Thêm danh sách roles vào JWT claims (để frontend sử dụng)
    extraClaims.put("roles", userDetails.user().getRoles().stream()
            .map(role -> role.getName())
            .toList());
    
    extraClaims.put("type", "access");

    Date now = new Date();
    Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());

    return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .claims(extraClaims)
            .subject(userDetails.getUsername())  // email
            .issuedAt(now)
            .expiration(expiry)
            .signWith(getSigningKey())
            .issuer("social-pulse-api")
            .compact();
}
```

**JWT Payload Example:**
```json
{
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "userId": 123,
  "roles": ["ADMIN"],
  "type": "access",
  "sub": "user@example.com",
  "iat": 1714104000,
  "exp": 1714107600,
  "iss": "social-pulse-api"
}
```

### Phase 3: Request Authentication & Authorization

#### 3.1. JWT Authentication Filter

**File**: `security/jwt/JwtAuthenticationFilter.java`

```java
@Override
protected void doFilterInternal(HttpServletRequest request, 
                                HttpServletResponse response, 
                                FilterChain filterChain) {
    // 1. Extract JWT từ Authorization header
    String jwt = extractJwtFromRequest(request);
    
    if (jwt != null) {
        // 2. Extract email từ JWT
        String email = jwtService.extractEmail(jwt);
        
        // 3. Load UserDetails từ database
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        
        // 4. Validate JWT
        if (jwtService.isTokenValid(jwt, userDetails)) {
            // 5. Tạo Authentication object với authorities
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()  // Authorities từ CustomUserDetails
                );
            
            // 6. Set vào SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
    
    filterChain.doFilter(request, response);
}
```

**Điều gì xảy ra:**
1. Filter extract JWT từ header `Authorization: Bearer <token>`
2. Parse JWT để lấy email (subject)
3. Load User từ database (với roles và permissions)
4. Validate JWT signature và expiration
5. Tạo `Authentication` object chứa authorities
6. Set vào `SecurityContext` để Spring Security sử dụng

#### 3.2. @PreAuthorize Authorization Check

**File**: `post/adapter/web/PostController.java`

```java
@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    // Kiểm tra role và permission
    @PostMapping
    @PreAuthorize("hasRole('USER') and hasAuthority('post:create')")
    public ResponseEntity<ApiResponse<PostCreationResponse>> createPost(
            @RequestBody @Valid PostCreationRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        // Chỉ user có ROLE_USER VÀ permission post:create mới vào được
        PostCreationResponse response = createPostUseCase.createPost(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.<PostCreationResponse>builder().data(response).build());
    }

    // Kiểm tra chỉ role
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') and hasAuthority('post:read')")
    public ResponseEntity<ApiResponse<ViewPostResponse>> viewPost(
            @PathVariable Long id, 
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(
            ApiResponse.<ViewPostResponse>builder()
                .data(viewPostUseCase.viewPost(id, currentUser))
                .build()
        );
    }

    // Kiểm tra nhiều roles
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long id, 
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        // USER hoặc ADMIN đều vào được
        // Logic bên trong sẽ kiểm tra thêm: USER chỉ xóa post của mình, ADMIN xóa bất kỳ
        deletePostUseCase.deletePost(id, currentUser);
        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .message("Post deleted successfully")
                .build()
        );
    }
}
```

**Spring Security Expression Language (SpEL):**

| Expression | Ý nghĩa |
|------------|---------|
| `hasRole('USER')` | User phải có ROLE_USER |
| `hasAnyRole('USER', 'ADMIN')` | User có ROLE_USER HOẶC ROLE_ADMIN |
| `hasAuthority('post:create')` | User phải có permission post:create |
| `hasAnyAuthority('post:delete', 'post:delete:any')` | User có một trong các permissions |
| `hasRole('USER') and hasAuthority('post:create')` | Phải có cả role VÀ permission |

### Phase 4: Programmatic Authorization Check

**File**: `post/application/service/DeletePostService.java`

```java
@Override
@Transactional
public void deletePost(Long postId, CustomUserDetails currentUser) {
    log.info("User {} is attempting to delete post {}", currentUser.getId(), postId);

    Post post = postRepository.findById(postId)
            .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

    // Kiểm tra xem user có phải là tác giả không
    boolean isAuthor = post.getUserId().equals(currentUser.getId());
    
    // Kiểm tra xem user có permission post:delete:any không (chỉ ADMIN mới có)
    boolean hasDeleteAnyPermission = currentUser.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("post:delete:any"));

    // USER chỉ xóa được post của mình
    // ADMIN xóa được bất kỳ post nào
    if (!isAuthor && !hasDeleteAnyPermission) {
        log.warn("User {} is not authorized to delete post {}", 
                 currentUser.getId(), postId);
        throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
    }

    postRepository.deleteById(postId);
    log.info("Post {} deleted successfully by user {}", postId, currentUser.getId());
}
```

**Khi nào dùng Programmatic Check:**
- Khi logic authorization phức tạp (ví dụ: kiểm tra ownership)
- Khi cần kết hợp nhiều điều kiện business logic
- Khi `@PreAuthorize` không đủ linh hoạt

## Role Hierarchy

**File**: `security/config/SecurityConfig.java`

```java
@Bean
public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy(
        "ROLE_ADMIN > ROLE_USER \n ROLE_USER > ROLE_GUEST"
    );
}
```

**Ý nghĩa:**
- `ROLE_ADMIN > ROLE_USER`: ADMIN tự động có tất cả quyền của USER
- `ROLE_USER > ROLE_GUEST`: USER tự động có tất cả quyền của GUEST

**Ví dụ:**
```java
@PreAuthorize("hasRole('USER')")
public void someMethod() {
    // ADMIN cũng vào được vì ADMIN > USER
}
```

## Permission Naming Convention

### Scope-Based Format: `resource:action[:modifier]`

| Permission | Ý nghĩa |
|------------|---------|
| `post:read` | Đọc posts |
| `post:create` | Tạo post mới |
| `post:update` | Cập nhật post của mình |
| `post:delete` | Xóa post của mình |
| `post:delete:any` | Xóa bất kỳ post nào (ADMIN) |
| `comment:create` | Tạo comment |
| `comment:delete` | Xóa comment của mình |
| `comment:delete:any` | Xóa bất kỳ comment nào (ADMIN) |
| `user:read` | Xem profile users |
| `user:update` | Cập nhật profile của mình |
| `user:manage` | Quản lý tất cả users (ADMIN) |
| `user:moderate` | Moderate users (ADMIN) |

### Lợi ích của Scope-Based Naming:
1. **Rõ ràng**: Dễ hiểu permission làm gì
2. **Nhất quán**: Theo pattern cố định
3. **Mở rộng**: Dễ thêm permissions mới
4. **RESTful**: Tương ứng với HTTP methods

## Default Roles & Permissions

### GUEST Role
```sql
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'GUEST' AND p.name IN ('post:read');
```
- Chỉ xem posts
- Không cần authentication

### USER Role
```sql
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'USER' AND p.name IN (
    'post:read',
    'post:create',
    'post:update',
    'post:delete',
    'comment:create',
    'comment:update',
    'comment:delete',
    'user:read',
    'user:update',
    'user:delete'
);
```
- Tất cả quyền của GUEST
- Tạo, sửa, xóa post/comment của mình
- Xem và cập nhật profile của mình

### ADMIN Role
```sql
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.name IN (
    'post:read',
    'post:create',
    'post:update',
    'post:delete',
    'post:delete:any',      -- Xóa bất kỳ post nào
    'comment:create',
    'comment:update',
    'comment:delete',
    'comment:delete:any',   -- Xóa bất kỳ comment nào
    'user:read',
    'user:update',
    'user:delete',
    'user:manage',          -- Quản lý users
    'user:moderate'         -- Moderate users
);
```
- Tất cả quyền của USER
- Xóa bất kỳ post/comment nào
- Quản lý và moderate users

### Permission Matrix

| Permission | GUEST | USER | ADMIN |
|---|---|---|---|
| `bookmark:create` | N | Y | Y |
| `bookmark:delete` | N | Y | Y |
| `bookmark:read` | N | Y | Y |
| `comment:create` | N | Y | Y |
| `comment:delete` | N | Y | Y |
| `comment:manage` | N | N | Y |
| `comment:react` | N | Y | Y |
| `comment:read` | N | Y | Y |
| `comment:update` | N | Y | Y |
| `discovery:read` | N | Y | Y |
| `feed:read` | N | Y | Y |
| `follow:create` | N | Y | Y |
| `follow:delete` | N | Y | Y |
| `follow:read` | N | Y | Y |
| `notification:read` | N | Y | Y |
| `notification:update` | N | Y | Y |
| `post:create` | N | Y | Y |
| `post:delete` | N | Y | Y |
| `post:manage` | N | N | Y |
| `post:react` | N | Y | Y |
| `post:read` | Y | Y | Y |
| `post:update` | N | Y | Y |
| `report:create` | N | Y | Y |
| `report:manage` | N | N | Y |
| `user:create` | N | Y | Y |
| `user:delete` | N | Y | Y |
| `user:manage` | N | N | Y |
| `user:moderate` | N | N | Y |
| `user:read` | N | Y | Y |
| `user:update` | N | Y | Y |

## Assign Role cho User Mới

**File**: `user/application/service/UserRoleService.java`

```java
@Service
public class UserRoleService {
    private final RoleRepository roleRepository;

    @Transactional
    public void assignDefaultRole(User user) {
        // Load role USER từ database
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException(
                    "Default USER role not found in database"
                ));

        // Thêm role vào user
        user.getRoles().add(userRole);
    }

    @Transactional
    public void assignRoles(User user, Set<String> roleNames) {
        user.getRoles().clear();

        roleNames.forEach(roleName -> {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Role not found: " + roleName
                    ));
            user.getRoles().add(role);
        });
    }
}
```

**Sử dụng trong CreateUserService:**

```java
@Override
@Transactional
public UserCreationResponse createUser(UserCreationRequest request) {
    // ... validation logic
    
    User user = userMapper.toUser(request, normalizedEmail, encodedPassword);
    user.applyDefaultState();

    // Assign default USER role
    userRoleService.assignDefaultRole(user);

    user = userRepository.save(user);
    
    return userMapper.toUserCreationResponse(user, "User created successfully");
}
```

## Testing Authorization

### Test với Different Roles

```java
@Test
void testCreatePost_WithUserRole_Success() {
    // Given: User có ROLE_USER và permission post:create
    CustomUserDetails userDetails = createUserWithRole("USER");
    
    // When: Gọi createPost
    ResponseEntity<ApiResponse<PostCreationResponse>> response = 
        postController.createPost(request, userDetails);
    
    // Then: Success
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
}

@Test
void testCreatePost_WithGuestRole_Forbidden() {
    // Given: User chỉ có ROLE_GUEST (không có post:create)
    CustomUserDetails guestDetails = createUserWithRole("GUEST");
    
    // When & Then: Throw AccessDeniedException
    assertThrows(AccessDeniedException.class, () -> {
        postController.createPost(request, guestDetails);
    });
}

@Test
void testDeleteAnyPost_WithAdminRole_Success() {
    // Given: ADMIN có permission post:delete:any
    CustomUserDetails adminDetails = createUserWithRole("ADMIN");
    Post otherUserPost = createPostByOtherUser();
    
    // When: ADMIN xóa post của người khác
    deletePostService.deletePost(otherUserPost.getId(), adminDetails);
    
    // Then: Success
    assertFalse(postRepository.existsById(otherUserPost.getId()));
}

@Test
void testDeleteAnyPost_WithUserRole_Forbidden() {
    // Given: USER không có permission post:delete:any
    CustomUserDetails userDetails = createUserWithRole("USER");
    Post otherUserPost = createPostByOtherUser();
    
    // When & Then: Throw AppException
    assertThrows(AppException.class, () -> {
        deletePostService.deletePost(otherUserPost.getId(), userDetails);
    });
}
```

## Quản Lý Roles & Permissions qua Database

### Thêm Permission Mới

```sql
-- Thêm permission mới
INSERT INTO permissions (name, description, created_at, updated_at)
VALUES ('post:pin', 'Pin post to top', NOW(), NOW());

-- Assign cho ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.name = 'post:pin';
```

### Tạo Role Mới

```sql
-- Tạo role MODERATOR
INSERT INTO roles (name, description, created_at, updated_at)
VALUES ('MODERATOR', 'Content moderator', NOW(), NOW());

-- Assign permissions cho MODERATOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'MODERATOR' AND p.name IN (
    'post:read',
    'post:delete:any',
    'comment:delete:any',
    'user:moderate'
);
```

### Thay Đổi Permissions của Role

```sql
-- Xóa permission khỏi role
DELETE FROM role_permissions
WHERE role_id = (SELECT id FROM roles WHERE name = 'USER')
  AND permission_id = (SELECT id FROM permissions WHERE name = 'post:delete');

-- Thêm permission vào role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'USER' AND p.name = 'post:share';
```

## Best Practices

### 1. Sử dụng @PreAuthorize cho Coarse-Grained Authorization
```java
// Kiểm tra role và permission cơ bản
@PreAuthorize("hasRole('USER') and hasAuthority('post:create')")
public void createPost() { }
```

### 2. Sử dụng Programmatic Check cho Fine-Grained Authorization
```java
// Kiểm tra ownership và business logic phức tạp
public void deletePost(Long postId, CustomUserDetails user) {
    boolean isAuthor = post.getUserId().equals(user.getId());
    boolean hasDeleteAny = user.getAuthorities().stream()
        .anyMatch(auth -> auth.getAuthority().equals("post:delete:any"));
    
    if (!isAuthor && !hasDeleteAny) {
        throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
    }
}
```

### 3. Đặt Tên Permission Rõ Ràng
```
✅ Good: post:delete:any, user:manage, comment:moderate
❌ Bad: delete_all, admin_user, mod
```

### 4. Sử dụng Role Hierarchy
```java
// Thay vì:
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")

// Dùng:
@PreAuthorize("hasRole('USER')")  // ADMIN tự động có quyền USER
```

### 5. Tách Biệt Domain và Infrastructure
```
✅ Domain models không phụ thuộc vào Spring Security
✅ Entities (infrastructure) có JPA annotations
✅ Mappers convert giữa domain và entities
```

## Troubleshooting

### Issue: @PreAuthorize không hoạt động
**Solution**: Kiểm tra `@EnableMethodSecurity` trong SecurityConfig
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← Cần có annotation này
public class SecurityConfig { }
```

### Issue: User không có permissions sau khi login
**Solution**: Kiểm tra EAGER fetch trong UserEntity
```java
@ManyToMany(fetch = FetchType.EAGER)  // ← Phải là EAGER
@JoinTable(name = "user_roles", ...)
private Set<RoleEntity> roles = new HashSet<>();
```

### Issue: ADMIN không có quyền USER
**Solution**: Kiểm tra RoleHierarchy bean
```java
@Bean
public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy(
        "ROLE_ADMIN > ROLE_USER \n ROLE_USER > ROLE_GUEST"
    );
}
```

### Issue: JWT không chứa roles
**Solution**: Kiểm tra JwtService.generateToken()
```java
extraClaims.put("roles", userDetails.user().getRoles().stream()
        .map(role -> role.getName())
        .toList());
```

## Tổng Kết

Hệ thống authorization của Social Pulse:

1. **Database-Driven**: Roles và permissions lưu trong database, dễ quản lý
2. **Scope-Based**: Permissions theo format `resource:action[:modifier]`
3. **Flexible**: Kết hợp declarative (`@PreAuthorize`) và programmatic checks
4. **Hierarchical**: Role hierarchy tự động thừa kế quyền
5. **Clean Architecture**: Tách biệt domain, application, infrastructure layers

Hệ thống này cho phép:
- Thêm/sửa/xóa roles và permissions mà không cần deploy lại
- Kiểm soát quyền truy cập ở nhiều mức độ (coarse và fine-grained)
- Mở rộng dễ dàng khi thêm features mới
- Test authorization logic một cách độc lập
