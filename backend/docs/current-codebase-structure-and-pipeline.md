# Backend Codebase Structure And Current Pipelines

Tai lieu nay mo ta lai `backend/src` theo code hien tai, khong dua tren y dinh thiet ke hay README cu.
Pham vi doc chinh:

- `backend/src/main/java/com/socialpulse/app`
- `backend/src/main/resources`
- `backend/src/test/java/com/socialpulse/app`

## 1. Tong quan nhanh

Backend hien tai la mot monolith Spring Boot MVC, chia theo module nghiep vu va theo lop:

- `adapter/web`: controller nhan HTTP request
- `application/usecase`: contract cho use case
- `application/service`: orchestration nghiep vu
- `domain/model`: entity/domain object
- `domain/repository`: abstraction cua persistence
- `adapter/persistence`: adapter noi domain repository voi JPA/JDBC/Redis
- `infrastructure/config`: wiring bean thu cong bang `@Configuration` + `@Bean`
- `infrastructure/persistence`: JPA entity, mapper, Spring Data repository

Stack chinh dang dung:

- Spring Boot 4 MVC
- Spring Security + JWT
- Spring Data JPA + Flyway + PostgreSQL
- Redis
- Spring Mail
- Cloudinary
- springdoc OpenAPI + Scalar page
- LightGBM local scorer cho feed ranking

Kieu thuc thi hien tai chu yeu la dong bo, request-response. Hai ngoai le du kien:

- OTP email co danh dau `@Async`
- share-count sync co `@Scheduled`

Tuy nhien, theo code hien tai chua thay `@EnableAsync` va chua thay `@EnableScheduling`, nen hai flow nay can duoc xem la "co code ho tro" hon la "da chac chan dang chay".

## 2. Entry points va runtime configuration

### 2.1. App entry

- `Application.java`
  - entry point duy nhat
  - chi co `@SpringBootApplication`

### 2.2. Profile va config

- `application.yaml`
  - mac dinh bat profile `dev`
- `application-dev.yaml`
  - datasource PostgreSQL
  - Flyway migration
  - JPA `ddl-auto=validate`
  - Redis host/port
  - SMTP config
  - JWT secret va TTL
  - LightGBM toggle va model location
  - Cloudinary config

### 2.3. Resources

- `db/migration`
  - 12 migration tu `V1__init.sql` den `V12__add_edited_to_comments.sql`
- `ai/README.md`
  - quy uoc artifact cho model LightGBM

## 3. Ban do package hien tai

So file theo module chinh:

- `user`: 40
- `auth`: 38
- `post`: 33
- `feed`: 29
- `comment`: 25
- `common`: 18
- `report`: 15
- `follow`: 14
- `ai`: 7
- `security`: 6

Cay package muc cao:

```text
com.socialpulse.app
|- ai
|  |- config
|  |- lightgbm
|  `- service
|- auth
|  |- adapter
|  |- application
|  |- domain
|  `- infrastructure
|- comment
|  |- adapter
|  |- application
|  |- domain
|  `- infrastructure
|- common
|  |- cloudinary
|  |- config
|  |- dto
|  |- exception
|  |- openapi
|  |- schedule
|  `- utils
|- feed
|  |- adapter
|  |- application
|  |- domain
|  `- infrastructure
|- follow
|  |- adapter
|  |- application
|  |- domain
|  `- infrastructure
|- post
|  |- adapter
|  |- application
|  |- domain
|  `- infrastructure
|- report
|  |- adapter
|  |- application
|  |- domain
|  `- infrastructure
|- security
|  |- config
|  |- encoder
|  |- jwt
|  `- user
`- user
   |- adapter
   |- application
   |- domain
   `- infrastructure
```

## 4. Pattern kien truc dang ap dung

Codebase hien tai theo huong "modular monolith + ports/adapters", nhung khong ep buoc 100% dong deu:

- module `user`, `auth`, `post`, `comment`, `follow`, `report` dung abstraction repository trong `domain/repository`
- adapter persistence da so di qua Spring Data JPA + MapStruct mapper
- module `feed` la module co tinh "orchestration" cao nhat
- `feed` khong di qua JPA repository thong thuong ma co `FeedRepositoryAdapter` dung `JdbcTemplate` de query candidate feed truc tiep
- wiring bean duoc khai bao thu cong trong tung `*Config.java`, khong dua hoan toan vao component scan cho service nghiep vu

Noi cach khac, cau truc hien tai uu tien:

- tach bien HTTP/controller
- tach service/use case
- che phu persistence bang adapter
- giu business flow tap trung trong service layer

## 5. Cac module nghiep vu

### 5.1. `auth`

Trach nhiem:

- dang ky
- xac minh email bang OTP
- dang nhap
- refresh token rotation
- logout
- quen mat khau / reset mat khau

Thanh phan chinh:

- `AuthController`
- `RegisterService`
- `VerifyEmailService`
- `AuthenticationService`
- `OtpService`
- `PasswordResetService`
- `JwtService`
- `RefreshTokenService`
- `RefreshTokenRevocationService`
- persistence adapter cho OTP Redis, email sender, refresh token DB

### 5.2. `user`

Trach nhiem:

- tao user
- gan role mac dinh
- doc user profile

Thanh phan chinh:

- `UserController`
- `CreateUserService`
- `GetUserProfileService`
- `UserRoleService`
- repository adapter cho `User`, `UserProfile`, `Role`, `Permission`

### 5.3. `post`

Trach nhiem:

- tao post
- xem post
- react upvote/downvote
- edit post
- delete post
- chia se post bang `parentPostId`

Thanh phan chinh:

- `PostController`
- `CreatePostService`
- `ViewPostService`
- `ReactPostService`
- `EditPostService`
- `DeletePostService`

### 5.4. `comment`

Trach nhiem:

- tao comment
- lay top-level comments theo `lastId` + `limit`
- sua comment
- xoa comment
- validate parent comment

### 5.5. `follow`

Trach nhiem:

- follow
- unfollow
- dem follow/follower
- lay tap user dang follow de phuc vu feed ranking

### 5.6. `feed`

Day la module trung tam cua pipeline ca nhan hoa feed.

Trach nhiem:

- chon candidate posts
- trich xuat feature
- ranking fallback
- ranking bang LightGBM neu bat
- cache feed theo user
- paginate ket qua

### 5.7. `report`

Trach nhiem:

- nhan report cho post/comment
- dat trang thai `PENDING`

### 5.8. `common`

Trach nhiem:

- `ApiResponse`
- exception va error response
- Redis/ObjectMapper config
- OpenAPI/Scalar docs page
- Cloudinary config/service
- scheduled sync

### 5.9. `security`

Trach nhiem:

- `SecurityFilterChain`
- `JwtAuthenticationFilter`
- `CustomUserDetailsService`
- `CustomUserDetails`
- password encoder
- JWT properties

### 5.10. `ai`

Trach nhiem:

- bind LightGBM config
- vectorize feature
- load JSON artifact
- local score cho feed ranking

## 6. Pipeline thuc thi tong quat

Pipeline chung cho request co auth:

1. HTTP request vao Spring MVC.
2. `SecurityFilterChain` ap dung stateless security.
3. `JwtAuthenticationFilter` doc `Authorization: Bearer <token>`.
4. Filter trich email tu access token, load `CustomUserDetails`, validate JWT, set `SecurityContext`.
5. Controller nhan request va `@AuthenticationPrincipal`.
6. Controller goi use case/service.
7. Service goi domain repository abstraction.
8. Adapter persistence noi sang JPA repository, JDBC, Redis, Mail, Cloudinary.
9. Response tra ve theo `ApiResponse<T>`.
10. Loi nghiep vu/validation duoc `GlobalExceptionHandler` chuan hoa thanh `ErrorResponse`.

## 7. Pipeline nghiep vu hien tai

### 7.1. Dang ky va xac minh email

Dang ky:

1. `POST /api/v1/auth/register`
2. `AuthController.registerUser`
3. `RegisterService.register`
4. `CreateUserService.createUser`
5. validate username/email/password confirm
6. encode password
7. `user.applyDefaultState()`
8. `UserRoleService.assignDefaultRole(user)` gan role `USER`
9. luu user vao PostgreSQL
10. `OtpService.generateToStoreAndSendEmail`
11. sinh OTP 6 so
12. hash OTP bang `AppPasswordEncoder`
13. luu payload OTP vao Redis key `auth:otp:<email>` TTL 300s
14. gui email HTML qua `EmailAdapter`

Xac minh email:

1. `POST /api/v1/auth/verify-email`
2. `VerifyEmailService.verifyEmail`
3. tim user theo email
4. neu da `ACTIVE` thi xoa OTP va return
5. verify OTP tu Redis
6. active account + danh dau verified
7. save user
8. xoa OTP khoi Redis

### 7.2. Login, refresh, logout

Login:

1. `POST /api/v1/auth/login`
2. `AuthenticationService.authenticate`
3. normalize email
4. load user tu DB
5. chan neu locked / chua verified / khong active
6. `AuthenticationManager.authenticate`
7. reset failed attempts + update `lastLoginAt`
8. tao access token JWT
9. tao refresh token random 64 bytes, hash SHA-256, luu DB
10. tra access token trong body
11. set refresh token vao HttpOnly cookie `sp_refresh_token`

Refresh:

1. `POST /api/v1/auth/refresh`
2. doc refresh token tu cookie
3. `RefreshTokenService.rotateTokens`
4. hash token va tim record trong DB
5. neu token revoked thi revoke toan bo active token cua user
6. neu hop le thi tao token moi
7. luu record moi, revoke record cu, set `replacedByToken`
8. tao access token moi va refresh cookie moi

Logout:

1. `POST /api/v1/auth/logout`
2. doc refresh token tu cookie
3. revoke current refresh token
4. tra cookie xoa bang `maxAge=0`

### 7.3. Quen mat khau

1. `forgot-password`: kiem tra user ton tai, sinh OTP, luu Redis, gui mail
2. `resend-otp`: tao lai OTP tuong tu
3. `verify-otp`: verify OTP, chua doi password
4. `reset-password`: verify OTP, encode password moi, save user, xoa OTP

### 7.4. User profile

Profile cua user hien tai:

1. `GET /api/v1/users/profile`
2. tao `UserViewProfileRequest` tu `currentUser.id`
3. `GetUserProfileService.getProfile`
4. query `UserProfileRepository`
5. map sang `UserViewProfileResponse`

Profile theo username:

1. `GET /api/v1/users/profile/{username}`
2. normalize username ve lower-case
3. query `UserProfileRepository.findByUsername`
4. map response

### 7.5. Post lifecycle

Tao post goc:

1. `POST /api/v1/posts`
2. `CreatePostService.createPost`
3. kiem tra user ton tai
4. neu `parentPostId == null` thi tao `PostType.ORIGINAL`
5. save post vao DB

Tao share post:

1. validate parent post ton tai
2. chan share cua share-post
3. chan share post private neu khong phai chu post
4. chan user share cung mot post nhieu lan
5. dang ky `afterCommit` hook
6. sau khi transaction commit, tang Redis key `post:<id>:shareCount:delta`
7. dong thoi add key vao Redis set `share:delta:keys`
8. save share-post vao DB voi `PostType.SHARE`

View post:

1. `GET /api/v1/posts/{postId}`
2. load post
3. chan neu da xoa hoac private ma khong phai chu so huu
4. map response

Luu y: hien tai view post chua tang `viewCount`.

React post:

1. `POST /api/v1/posts/react`
2. load post va user
3. tim reaction hien tai cua user tren post
4. neu chua co thi tao moi va tang counter
5. neu reaction giong reaction cu thi xoa reaction va giam counter
6. neu reaction khac thi doi loai reaction va update counters
7. save reaction va save post

Edit post:

1. `PUT /api/v1/posts/{postId}`
2. load post
3. goi `post.update(...)`
4. save lai DB

Delete post:

1. `DELETE /api/v1/posts/{postId}`
2. load post
3. cho phep neu la author hoac co authority `post:delete:any`
4. adapter hien tai goi `jpaPostRepository.deleteById`

Luu y: controller comment ghi "soft delete", nhung persistence hien tai dang xoa theo `deleteById`, nen hanh vi thuc te can duoc xem la hard delete cho toi khi repository/entity chung minh nguoc lai.

### 7.6. Comment lifecycle

Create comment:

1. `POST /api/v1/posts/{postId}/comments`
2. kiem tra post ton tai
3. kiem tra user ton tai
4. `ValidateParentCommentUseCase` neu la reply
5. map request thanh `Comment`
6. save DB

Lay top-level comments:

1. `GET /api/v1/posts/{postId}/comments?lastId=&limit=`
2. query `JpaCommentRepository.findTopLevelCommentsByPostId`
3. filter `deleted = false`
4. phan trang theo `lastId` giam dan

Update comment:

1. load comment
2. dam bao comment thuoc dung post
3. dam bao current user la owner
4. chan sua comment da xoa
5. cap nhat content va save

Delete comment:

1. load comment
2. dam bao dung post + dung owner
3. neu chua xoa thi `comment.markDeleted()`
4. save lai

Comment hien tai la soft delete o domain layer.

### 7.7. Follow lifecycle

Follow:

1. `POST /api/v1/follows/{userId}`
2. chan self-follow
3. dam bao target user ton tai
4. chan duplicate follow
5. save relation

Unfollow:

1. `DELETE /api/v1/follows/{userId}`
2. dam bao relation ton tai
3. xoa relation

### 7.8. Report lifecycle

1. `POST /api/reports`
2. lay `currentUser.id`
3. map request thanh `Report`
4. `report.markPending()`
5. save DB

Flow hien tai chua thay validation sau:

- post/comment bi report co ton tai hay khong
- user co quyen report target nay hay khong
- duplicate report policy

### 7.9. Feed ranking pipeline

Day la pipeline quan trong nhat cua backend hien tai.

API:

- `GET /api/v1/feed?page=0&size=20`

Flow:

1. `FeedController.getFeed`
2. `GetFeedService.getFeed`
3. `FeedRankingService.getPaginatedFeed`
4. `FeedRankingService.getRankedFeed`
5. check Redis cache `user:feed:<userId>`
6. neu cache hop le thi dung cache va paginate
7. neu cache miss thi `CandidateSelectionService.selectCandidates`
8. tron candidate tu 4 nguon:
   - recent public posts trong 7 ngay, toi da 200
   - posts cua user dang follow trong 7 ngay, toi da 100
   - popular posts trong 7 ngay, toi da 100
   - random public posts, toi da 100
9. loai duplicate bang `seenIds`
10. ranking tam thoi bang `FallbackRankingService`
11. `FeatureExtractionService.extractFeatures`
12. batch load user, post-count, followed-author set
13. sinh feature cho viewer, author, interaction, post
14. cache `user:features:<userId>` trong Redis 10 phut
15. tao `RankingRequest`
16. goi `PredictRankingUseCase`
17. neu LightGBM duoc bat va model hop le thi score tung post bang local scorer
18. neu prediction khong hop le thi giu fallback ranking
19. sort score giam dan
20. map thanh `FeedItem`
21. cache toan bo feed vao Redis 10 phut
22. cat trang theo `page` va `size`

Feature hien tai:

- `PostFeatures`: content length, image flag, share flag, age, hot score, vote counts, comment/share/view counts...
- `AuthorFeatures`: post count, account age, engagement rate mac dinh 0.0
- `ViewerFeatures`: post count, account age
- `InteractionFeatures`: interaction count 7d/30d hien dang hardcode 0, affinity dua tren follow

Y nghia kien truc:

- feed pipeline da mo duong cho ML ranking
- nhung du lieu behavior realtime van rat han che
- neu ML fail, he thong van co deterministic ranking

### 7.10. LightGBM local scoring pipeline

1. doc config `ai.lightgbm.*`
2. neu `enabled=false` thi bo qua model, feed dung fallback
3. neu bat, `LightGbmRankingService` se lazy-load model tu `classpath:ai/lightgbm-ranking-model.json`
4. chap nhan 2 format:
   - raw `Booster.dump_model()`
   - wrapped artifact co `model_dump`
5. validate `feature_schema_version`
6. `LightGbmFeatureVectorizer` chuyen `RankingFeatures` thanh feature map
7. `LightGbmModelScorer` duyet tree JSON va tinh score
8. tra `RankingResponse` ve cho `FeedRankingService`

## 8. Data va state hien tai

### 8.1. PostgreSQL

Luu state chinh:

- user, role, permission, user_profile
- post, post_reactions
- comment, comment_reactions
- follows
- refresh_tokens
- reports

### 8.2. Redis

Luu state tam thoi va cache:

- `auth:otp:<email>`: OTP payload, TTL 5 phut
- `user:feed:<userId>`: cached ranked feed, TTL 10 phut
- `user:features:<userId>`: cached user features, TTL 10 phut
- `post:<id>:shareCount:delta`: delta share count
- `share:delta:keys`: set theo doi cac delta key can sync

## 9. Scheduling va background flow

`SyncSchedule.syncShareCount()` du kien chay moi 10 giay:

1. lay tap key tu `share:delta:keys`
2. doc va reset moi delta key ve `0`
3. gom thanh `Map<postId, delta>`
4. goi `postRepository.updateShareCount(updates)`
5. neu thanh cong thi xoa key khoi set theo doi
6. neu that bai thi cong lai delta ve Redis

Flow nay co y nghia giam write-amplification khi share post, nhung co 3 diem can luu y:

- chua thay `@EnableScheduling`
- `JpaPostRepository.updateShareCount(...)` dung `@Query("UPDATE ...")` nhung chua thay `@Modifying`
- logic `if (updates.isEmpty()) return;` dang nam trong vong `for`, nen co the dung som hon du kien

Vi vay, day la pipeline co y tuong ro rang, nhung can verify lai runtime.

## 10. Security model hien tai

### 10.1. URL-level security

- permit all:
  - `/api/v1/auth/**`
  - `/v3/api-docs/**`
  - `/swagger-ui/**`
  - `/swagger-ui.html`
  - `/scalar/**`
- con lai bat buoc authenticated

### 10.2. Method-level security

Da bat `@EnableMethodSecurity`, va controller/service dung `@PreAuthorize` theo role/permission.

### 10.3. Authority model

`CustomUserDetails` build authority tu:

- `ROLE_<roleName>`
- permission names trong role

Role hierarchy:

- `ROLE_ADMIN > ROLE_USER`
- `ROLE_USER > ROLE_GUEST`

## 11. API surface hien tai

Controller dang expose cac nhom endpoint:

- `/api/v1/auth`
- `/api/v1/users`
- `/api/v1/posts`
- `/api/v1/posts/{postId}/comments`
- `/api/v1/follows`
- `/api/v1/feed`
- `/api/reports`
- `/scalar`
- `/v3/api-docs`

## 12. Test coverage hien tai

Test hien co trong `src/test/java`:

- `AppApplicationTests`
- `LightGbmModelScorerTest`
- `LightGbmRankingServiceTest`
- `FeedRankingServiceTest`
- `GetFeedServiceTest`

Nhan xet:

- feed va AI ranking la phan duoc test ky nhat
- chua thay controller integration test
- chua thay test cho auth, post, comment, follow, report

## 13. Cac diem quan trong can ghi nho khi tiep tuc phat trien

1. Codebase hien tai co cau truc module ro, de mo rong theo feature.
2. `feed` la noi phuc tap nhat va la trung tam cua "smart social media".
3. Redis dang duoc dung cho ca OTP, cache feed, cache feature va share-count delta.
4. Hanh vi runtime thuc te cua background tasks can kiem tra lai truoc khi dua vao production.
5. Co mot so cho annotation/comment mo ta khac voi hanh vi persistence thuc te, vi vay khi sua feature nen doc ca service va adapter/repository, khong chi doc controller.

## 14. Tom tat ngan

Neu tom tat codebase hien tai trong 3 cau:

- Day la Spring Boot modular monolith, nghiêng ve ports/adapters, voi user/auth/post/comment/follow/report/feed tach thanh module rieng.
- Pipeline quan trong nhat la pipeline feed: cache -> candidate selection -> feature extraction -> LightGBM prediction hoac deterministic fallback -> cache -> paginate.
- He thong da co dat nen cho ML ranking va background syncing bang Redis, nhung mot vai background capability hien van can xac minh wiring de dam bao chay dung trong runtime.
