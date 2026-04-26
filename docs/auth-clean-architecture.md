# Auth Module - Clean Architecture Structure

## Tổng Quan

Auth module quản lý authentication và authorization business logic, bao gồm: login, register, JWT token management, OTP verification, password reset.

## Cấu Trúc Clean Architecture

```
auth/
├── domain/                          # Business logic core (không phụ thuộc gì)
│   ├── model/                       # Domain models
│   │   ├── Otp.java                # OTP domain model
│   │   └── RefreshToken.java      # Refresh token domain model
│   └── repository/                  # Repository interfaces (ports)
│       ├── OtpRepository.java
│       └── RefreshTokenRepository.java
│
├── application/                     # Use cases và business rules
│   ├── usecase/                    # Use case interfaces
│   │   ├── AuthenticationUseCase.java
│   │   ├── RegisterUseCase.java
│   │   ├── VerifyEmailUseCase.java
│   │   ├── JwtUseCase.java
│   │   ├── RefreshTokenUseCase.java
│   │   ├── RefreshTokenRevocationUseCase.java
│   │   ├── OtpUseCase.java
│   │   └── PasswordResetUseCase.java
│   │
│   ├── service/                    # Use case implementations
│   │   ├── AuthenticationService.java
│   │   ├── RegisterService.java
│   │   ├── VerifyEmailService.java
│   │   ├── jwt/
│   │   │   ├── JwtService.java
│   │   │   ├── RefreshTokenService.java
│   │   │   └── RefreshTokenRevocationService.java
│   │   ├── otp/
│   │   │   └── OtpService.java
│   │   └── password/
│   │       └── PasswordResetService.java
│   │
│   ├── dto/                        # Data Transfer Objects
│   │   ├── request/
│   │   │   ├── LoginRequest.java
│   │   │   ├── EmailVerificationRequest.java
│   │   │   ├── VerifyOtpRequest.java
│   │   │   ├── ResendOtpRequest.java
│   │   │   ├── ForgotPasswordRequest.java
│   │   │   └── ResetPasswordRequest.java
│   │   ├── response/
│   │   │   └── LoginResponse.java
│   │   ├── mapper/
│   │   │   └── AuthMapper.java
│   │   └── TokenPair.java
│   │
│   └── port/                       # Output ports (interfaces)
│       └── EmailPort.java          # Interface cho email service
│
├── infrastructure/                  # Technical implementations
│   ├── config/
│   │   └── AuthConfig.java        # Spring configuration cho auth beans
│   │
│   └── persistence/                # Database implementation
│       ├── entity/
│       │   └── RefreshTokenEntity.java
│       ├── mapper/
│       │   └── RefreshTokenMapper.java
│       └── repository/
│           └── JpaRefreshTokenRepository.java
│
└── adapter/                        # Adapters (glue code)
    ├── web/                        # Input adapters
    │   └── AuthController.java    # REST API endpoints
    │
    └── persistence/                # Output adapters
        ├── RefreshTokenRepositoryAdapter.java
        ├── OtpStorageAdapter.java
        └── EmailAdapter.java
```

## Security Module (Tách Riêng)

```
security/                           # Infrastructure chung cho toàn app
├── config/
│   └── SecurityConfig.java        # Spring Security configuration
├── encoder/
│   └── AppPasswordEncoder.java   # Password encoder bean
├── jwt/
│   ├── JwtAuthenticationFilter.java
│   └── JwtProperties.java
└── user/
    ├── CustomUserDetails.java
    └── CustomUserDetailsService.java
```

## Dependency Flow (Clean Architecture)

```
┌─────────────────────────────────────────────────────────────┐
│                        Adapter Layer                         │
│  (Web Controllers, Repository Adapters, Email Adapter)      │
└────────────────────────┬────────────────────────────────────┘
                         │ depends on
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                        │
│     (Use Cases, Services, DTOs, Ports/Interfaces)           │
└────────────────────────┬────────────────────────────────────┘
                         │ depends on
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                       Domain Layer                           │
│           (Models, Repository Interfaces)                    │
│              (NO dependencies)                               │
└─────────────────────────────────────────────────────────────┘
                         ↑
                         │ depends on
                         │
┌─────────────────────────────────────────────────────────────┐
│                   Infrastructure Layer                       │
│    (JPA Entities, Mappers, JPA Repositories, Config)       │
└─────────────────────────────────────────────────────────────┘
```

## Chi Tiết Từng Layer

### 1. Domain Layer

**Mục đích**: Core business logic, không phụ thuộc vào framework hay infrastructure.

**domain/model/Otp.java**
```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Otp {
    private String email;
    private String code;
    private LocalDateTime expiresAt;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
```

**domain/model/RefreshToken.java**
```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    private Long id;
    private String token;
    private Long userId;
    private LocalDateTime expiresAt;
    private boolean revoked;
    private LocalDateTime createdAt;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
```

**domain/repository/RefreshTokenRepository.java** (Port)
```java
public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByToken(String token);
    void revokeAllByUserId(Long userId);
}
```

### 2. Application Layer

**Mục đích**: Orchestrate business logic, implement use cases.

**application/usecase/AuthenticationUseCase.java**
```java
public interface AuthenticationUseCase {
    LoginResponse login(LoginRequest request);
    TokenPair refreshToken(String refreshToken);
}
```

**application/service/AuthenticationService.java**
```java
public class AuthenticationService implements AuthenticationUseCase {
    private final AuthenticationManager authenticationManager;
    private final JwtUseCase jwtUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. Authenticate user
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(), 
                request.getPassword()
            )
        );
        
        // 2. Load user details
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
        // 3. Generate tokens
        String accessToken = jwtUseCase.generateToken(userDetails);
        String refreshToken = refreshTokenUseCase.createRefreshToken(userDetails);
        
        // 4. Return response
        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtUseCase.getAccessExpirationMs())
            .build();
    }
}
```

**application/port/EmailPort.java** (Output Port)
```java
public interface EmailPort {
    void sendOtpEmail(String to, String otp);
    void sendPasswordResetEmail(String to, String resetLink);
}
```

### 3. Infrastructure Layer

**Mục đích**: Technical implementations (database, external services).

**infrastructure/persistence/entity/RefreshTokenEntity.java**
```java
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private boolean revoked;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
```

**infrastructure/persistence/mapper/RefreshTokenMapper.java**
```java
@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {
    RefreshToken toDomain(RefreshTokenEntity entity);
    RefreshTokenEntity toEntity(RefreshToken domain);
}
```

**infrastructure/config/AuthConfig.java**
```java
@Configuration
public class AuthConfig {
    
    @Bean
    public RefreshTokenRepository refreshTokenRepository(
            JpaRefreshTokenRepository jpaRepo,
            RefreshTokenMapper mapper) {
        return new RefreshTokenRepositoryAdapter(jpaRepo, mapper);
    }
    
    @Bean
    public AuthenticationUseCase authenticationUseCase(
            AuthenticationManager authManager,
            JwtUseCase jwtUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            CustomUserDetailsService userDetailsService) {
        return new AuthenticationService(
            authManager, jwtUseCase, refreshTokenUseCase, userDetailsService
        );
    }
    
    @Bean
    public JwtUseCase jwtUseCase(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }
    
    // ... other beans
}
```

### 4. Adapter Layer

**Mục đích**: Connect external world với application layer.

**adapter/web/AuthController.java** (Input Adapter)
```java
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {
    private final AuthenticationUseCase authenticationUseCase;
    private final RegisterUseCase registerUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request) {
        LoginResponse response = authenticationUseCase.login(request);
        return ResponseEntity.ok(
            ApiResponse.<LoginResponse>builder()
                .data(response)
                .build()
        );
    }
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserCreationResponse>> register(
            @RequestBody @Valid UserCreationRequest request) {
        UserCreationResponse response = registerUseCase.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.<UserCreationResponse>builder()
                .data(response)
                .build()
            );
    }
}
```

**adapter/persistence/RefreshTokenRepositoryAdapter.java** (Output Adapter)
```java
@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {
    private final JpaRefreshTokenRepository jpaRepository;
    private final RefreshTokenMapper mapper;
    
    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity entity = mapper.toEntity(token);
        RefreshTokenEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token)
            .map(mapper::toDomain);
    }
    
    @Override
    public void revokeAllByUserId(Long userId) {
        jpaRepository.revokeAllByUserId(userId);
    }
}
```

**adapter/persistence/EmailAdapter.java** (Output Adapter)
```java
@Component
public class EmailAdapter implements EmailPort {
    private final JavaMailSender mailSender;
    
    @Override
    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Email Verification OTP");
        message.setText("Your OTP code is: " + otp);
        mailSender.send(message);
    }
    
    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset");
        message.setText("Click here to reset password: " + resetLink);
        mailSender.send(message);
    }
}
```

## Luồng Request (Example: Login)

```
1. HTTP Request
   ↓
2. AuthController.login()                    [Adapter Layer]
   ↓
3. AuthenticationService.login()             [Application Layer]
   ├─→ AuthenticationManager.authenticate()  [Spring Security]
   ├─→ JwtService.generateToken()           [Application Layer]
   └─→ RefreshTokenService.createToken()    [Application Layer]
       ↓
4. RefreshTokenRepositoryAdapter.save()      [Adapter Layer]
   ↓
5. JpaRefreshTokenRepository.save()          [Infrastructure Layer]
   ↓
6. Database
```

## Dependency Injection Flow

```
AuthConfig (Infrastructure)
    ↓ creates beans
    ↓
AuthenticationService (Application)
    ↓ depends on
    ↓
RefreshTokenRepository interface (Domain)
    ↑ implemented by
    ↑
RefreshTokenRepositoryAdapter (Adapter)
    ↓ depends on
    ↓
JpaRefreshTokenRepository (Infrastructure)
```

## Lợi Ích Clean Architecture

### 1. Independence of Frameworks
- Domain layer không phụ thuộc Spring, JPA
- Dễ test domain logic mà không cần framework

### 2. Testability
```java
// Test domain logic
@Test
void testRefreshTokenExpired() {
    RefreshToken token = RefreshToken.builder()
        .expiresAt(LocalDateTime.now().minusHours(1))
        .build();
    
    assertTrue(token.isExpired());
}

// Test use case với mock
@Test
void testLogin() {
    // Mock dependencies
    AuthenticationManager authManager = mock(AuthenticationManager.class);
    JwtUseCase jwtUseCase = mock(JwtUseCase.class);
    
    // Test service
    AuthenticationService service = new AuthenticationService(
        authManager, jwtUseCase, ...
    );
    
    LoginResponse response = service.login(request);
    assertNotNull(response.getAccessToken());
}
```

### 3. Independence of Database
- Dễ thay đổi từ PostgreSQL sang MongoDB
- Chỉ cần implement lại adapter và infrastructure layer

### 4. Independence of UI
- Business logic không biết gì về REST API
- Có thể thêm GraphQL, gRPC mà không sửa business logic

### 5. Maintainability
- Mỗi layer có responsibility rõ ràng
- Dễ tìm và sửa bugs
- Dễ thêm features mới

## Best Practices

### 1. Domain Layer
- ✅ Pure Java objects (POJOs)
- ✅ Business logic methods
- ❌ Không có annotations (@Entity, @Service, @Controller)
- ❌ Không import Spring, JPA

### 2. Application Layer
- ✅ Use case interfaces
- ✅ Service implementations
- ✅ DTOs cho input/output
- ✅ Port interfaces cho external services
- ❌ Không có JPA entities
- ❌ Không có HTTP concerns

### 3. Infrastructure Layer
- ✅ JPA entities với annotations
- ✅ JPA repositories
- ✅ Mappers giữa domain và entities
- ✅ Configuration beans
- ❌ Không có business logic

### 4. Adapter Layer
- ✅ Controllers (input adapters)
- ✅ Repository adapters (output adapters)
- ✅ External service adapters
- ❌ Không có business logic
- ❌ Chỉ delegate sang application layer

## So Sánh: Auth vs User Module

### Auth Module
- **Focus**: Authentication & authorization business logic
- **Domain**: Otp, RefreshToken
- **Use Cases**: Login, Register, Verify Email, Reset Password
- **External**: Email service

### User Module
- **Focus**: User management business logic
- **Domain**: User, UserProfile, Role, Permission
- **Use Cases**: Create User, Get Profile, Manage Roles
- **External**: None (pure domain)

### Security Module (Infrastructure)
- **Focus**: Spring Security configuration
- **Components**: CustomUserDetails, JwtFilter, SecurityConfig
- **Shared**: Được dùng bởi cả Auth và User modules

## Migration Guide

Nếu có code cũ không theo clean architecture:

### Before (Messy)
```
auth/
├── controller/
│   └── AuthController.java (có business logic)
├── service/
│   └── AuthService.java (trực tiếp dùng JPA entities)
└── entity/
    └── RefreshTokenEntity.java (có business logic)
```

### After (Clean)
```
auth/
├── domain/
│   ├── model/RefreshToken.java (business logic)
│   └── repository/RefreshTokenRepository.java (interface)
├── application/
│   ├── usecase/AuthenticationUseCase.java
│   └── service/AuthenticationService.java (pure business logic)
├── infrastructure/
│   └── persistence/entity/RefreshTokenEntity.java (chỉ JPA)
└── adapter/
    ├── web/AuthController.java (chỉ HTTP mapping)
    └── persistence/RefreshTokenRepositoryAdapter.java
```

## Tổng Kết

Clean Architecture trong Auth module:

1. **Domain Layer**: Core business models (Otp, RefreshToken)
2. **Application Layer**: Use cases và services (Login, Register, JWT)
3. **Infrastructure Layer**: Technical implementations (JPA, Email)
4. **Adapter Layer**: Glue code (Controllers, Repository adapters)
5. **Security Module**: Shared infrastructure (Spring Security config)

Cấu trúc này đảm bảo:
- Business logic độc lập với framework
- Dễ test
- Dễ maintain
- Dễ mở rộng
- Tuân thủ SOLID principles
