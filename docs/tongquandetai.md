# TỔNG QUAN ĐỀ TÀI

## 1. GIỚI THIỆU DỰ ÁN

**Social Pulse** là một nền tảng mạng xã hội thông minh được hỗ trợ bởi trí tuệ nhân tạo (AI), tập trung vào việc cá nhân hóa trải nghiệm người dùng thông qua hệ thống xếp hạng nội dung thông minh. Đây là một ứng dụng full-stack hiện đại với kiến trúc sạch (Clean Architecture) và các tính năng mạng xã hội toàn diện.

### Mục tiêu chính của dự án:
- Xây dựng nền tảng mạng xã hội với trải nghiệm người dùng được cá nhân hóa
- Ứng dụng machine learning để xếp hạng và đề xuất nội dung phù hợp với từng người dùng
- Cung cấp các tính năng mạng xã hội đầy đủ: đăng bài, bình luận, theo dõi, nhắn tin real-time
- Đảm bảo bảo mật và phân quyền chặt chẽ với hệ thống RBAC (Role-Based Access Control)
- Xây dựng hệ thống có khả năng mở rộng và dễ bảo trì

---

## 2. CÔNG NGHỆ SỬ DỤNG

### 2.1. Frontend
- **React 19**: Framework JavaScript hiện đại cho xây dựng giao diện người dùng
- **Vite 8**: Build tool nhanh chóng với Hot Module Replacement
- **TypeScript**: Ngôn ngữ lập trình có type-safety, giảm lỗi runtime
- **Tailwind CSS 4**: Framework CSS utility-first cho styling nhanh chóng
- **React Router 7**: Quản lý routing và navigation
- **Axios**: HTTP client cho giao tiếp với backend API
- **WebSocket** (@stomp/stompjs, sockjs-client): Hỗ trợ tính năng real-time
- **Shadcn UI + Radix UI**: Thư viện component UI có accessibility cao
- **Framer Motion**: Thư viện animation mượt mà
- **Zod**: Schema validation cho form và data

### 2.2. Backend
- **Java 21**: Phiên bản Java LTS mới nhất với các tính năng hiện đại
- **Spring Boot 4.0.6**: Framework Java phổ biến cho xây dựng ứng dụng enterprise
- **Spring Security**: Bảo mật với JWT authentication
- **Spring Data JPA**: ORM framework cho tương tác với database
- **Spring WebSocket**: Hỗ trợ giao tiếp real-time
- **PostgreSQL**: Hệ quản trị cơ sở dữ liệu quan hệ mạnh mẽ
- **Redis**: In-memory database cho caching và lưu trữ tạm thời
- **Flyway**: Quản lý database migration có version control
- **MapStruct**: Object mapping tự động giữa các layer
- **Lombok**: Giảm boilerplate code
- **Cloudinary**: Dịch vụ lưu trữ và xử lý media (ảnh, video)
- **SpringDoc OpenAPI**: Tự động generate API documentation

### 2.3. AI Pipeline
- **Python 3.12**: Ngôn ngữ lập trình cho machine learning
- **FastAPI**: Web framework hiệu suất cao cho inference API
- **scikit-learn**: Thư viện machine learning với Gradient Boosted Decision Trees
- **NumPy**: Thư viện tính toán số học
- **Zstandard**: Nén dữ liệu hiệu quả cho training data
- **Reddit Dataset**: Dữ liệu huấn luyện từ Reddit (submissions + comments)

---

## 3. KIẾN TRÚC HỆ THỐNG

### 3.1. Kiến trúc Backend: Clean Architecture (Hexagonal Architecture)

Backend được xây dựng theo mô hình **modular monolith** với sự phân tách rõ ràng giữa các layer:

#### Cấu trúc layer:
```
adapter/web          → REST controllers, xử lý HTTP requests
application/service  → Business logic, orchestration, use cases
domain/model         → Core entities, repository interfaces
infrastructure/      → Implementations cụ thể (persistence, config)
```

#### Lợi ích của kiến trúc này:
- **Testability**: Dễ dàng test từng layer độc lập
- **Maintainability**: Code rõ ràng, dễ bảo trì và mở rộng
- **Separation of Concerns**: Mỗi layer có trách nhiệm riêng biệt
- **Flexibility**: Dễ dàng thay đổi implementation mà không ảnh hưởng business logic

### 3.2. Các Module Chính (17 modules)

1. **auth**: Xác thực và phân quyền (JWT, OTP, refresh token)
2. **user**: Quản lý người dùng và profile
3. **post**: Tạo, chỉnh sửa, xóa bài viết, reactions
4. **comment**: Hệ thống bình luận với nested replies
5. **feed**: Feed cá nhân hóa với AI ranking
6. **follow**: Hệ thống theo dõi người dùng
7. **bookmark**: Lưu bài viết yêu thích
8. **block**: Chặn người dùng
9. **chat**: Nhắn tin real-time với WebSocket
10. **notification**: Hệ thống thông báo
11. **discovery**: Tìm kiếm và khám phá nội dung
12. **topic**: Quản lý chủ đề/danh mục
13. **report**: Báo cáo và kiểm duyệt nội dung
14. **admin**: Dashboard quản trị hệ thống
15. **realtime**: Điều phối các tính năng real-time
16. **common**: Utilities và configurations dùng chung
17. **security**: Cấu hình bảo mật

### 3.3. Kiến trúc Frontend

Frontend được tổ chức theo mô hình **component-based architecture**:

```
pages/       → Các trang chính của ứng dụng
components/  → Reusable UI components
services/    → API communication layer
hooks/       → Custom React hooks
utils/       → Helper functions
```

---

## 4. CÁC TÍNH NĂNG CHÍNH

### 4.1. Xác thực và Phân quyền

#### JWT Authentication:
- **Access Token**: Thời hạn 15 phút
- **Refresh Token**: Thời hạn 7 ngày với rotation mechanism
- Token được validate với database để có thể revoke ngay lập tức

#### Email Verification:
- OTP (One-Time Password) được gửi qua email
- Lưu trữ trong Redis với TTL 5 phút
- Bắt buộc verify email trước khi sử dụng đầy đủ tính năng

#### Password Reset:
- Flow reset mật khẩu an toàn với OTP
- Token có thời hạn ngắn để tăng bảo mật

#### RBAC (Role-Based Access Control):
- **3 Roles**: GUEST, USER, ADMIN
- **Permission-based authorization**: Phân quyền chi tiết đến từng action
- **Method-level security**: Sử dụng `@PreAuthorize` annotation
- **Ownership checks**: Kiểm tra quyền sở hữu tài nguyên ở service layer

### 4.2. Tính năng Mạng xã hội

#### Posts (Bài viết):
- Tạo, chỉnh sửa, xóa bài viết
- **Share posts**: Chia sẻ bài viết của người khác (parent_post_id)
- **Reactions**: Upvote/Downvote
- **Multimedia support**: Hỗ trợ ảnh và video
- **Topics**: Gắn thẻ chủ đề cho bài viết

#### Comments (Bình luận):
- Hệ thống bình luận với **nested replies** (trả lời bình luận)
- Reactions cho comments
- Soft delete để giữ cấu trúc thread

#### Social Interactions:
- **Follow/Unfollow**: Theo dõi người dùng
- **Bookmarks**: Lưu bài viết yêu thích
- **Block users**: Chặn người dùng không mong muốn
- **User interactions tracking**: Theo dõi tương tác để tính affinity score

### 4.3. Feed Cá nhân hóa với AI (Tính năng nổi bật)

Đây là **tính năng đặc trưng** của Social Pulse, sử dụng machine learning để xếp hạng và đề xuất nội dung phù hợp với từng người dùng.

#### Pipeline xếp hạng feed:

**Bước 1: Candidate Selection (Chọn ứng viên)**
Hệ thống thu thập bài viết từ 4 nguồn:
- **Recent posts**: Bài viết công khai gần đây (7 ngày, max 200)
- **Following posts**: Bài viết từ người dùng đang theo dõi (7 ngày, max 100)
- **Popular posts**: Bài viết phổ biến (7 ngày, max 100)
- **Random posts**: Bài viết ngẫu nhiên để tăng diversity (max 100)

**Bước 2: Feature Extraction (Trích xuất đặc trưng)**
Hệ thống tính toán **19 features** cho mỗi bài viết:

*Post Features (12 features):*
- `content_length`: Độ dài nội dung
- `has_multimedia`: Có ảnh/video hay không
- `is_share_post`: Là bài chia sẻ hay không
- `post_age_hours`: Tuổi của bài viết (giờ)
- `hot_score`: Điểm "hot" dựa trên engagement và thời gian
- `upvote_ratio`: Tỷ lệ upvote/(upvote+downvote)
- `upvote_count`, `downvote_count`: Số lượng reactions
- `comment_count`, `share_count`, `view_count`: Số lượng tương tác
- `popularity`: Điểm phổ biến tổng hợp

*Author Features (3 features):*
- `author_seniority`: Thâm niên của tác giả (ngày)
- `author_post_count`: Số bài viết của tác giả
- `author_engagement_rate`: Tỷ lệ engagement trung bình

*Interaction Features (4 features):*
- `interaction_count_7d`: Số tương tác trong 7 ngày
- `interaction_count_30d`: Số tương tác trong 30 ngày
- `hours_since_last_interaction`: Thời gian từ lần tương tác cuối
- `affinity_score`: Điểm thân thiết với tác giả

**Bước 3: ML Ranking (Xếp hạng bằng Machine Learning)**
- Model: **Gradient Boosted Decision Trees** (scikit-learn)
- Input: 19 features đã được normalize và transform
- Output: Relevance score cho mỗi bài viết
- **Fallback mechanism**: Nếu AI service không khả dụng, sử dụng deterministic ranking

**Bước 4: Caching (Lưu cache)**
- **Feed cache**: Lưu kết quả feed đã xếp hạng trong Redis (TTL 10 phút)
- **Feature cache**: Lưu features đã tính toán (TTL 10 phút)
- Giảm tải cho database và AI service

#### Lợi ích của AI-powered feed:
- Trải nghiệm cá nhân hóa cho từng người dùng
- Tăng engagement và thời gian sử dụng
- Khám phá nội dung phù hợp với sở thích
- Cân bằng giữa nội dung từ người theo dõi và nội dung mới

### 4.4. Real-time Features

#### WebSocket Chat:
- Nhắn tin real-time giữa người dùng
- **Conversations**: Quản lý các cuộc hội thoại
- **Messages**: Tin nhắn với timestamp và read status
- Protocol: **STOMP over SockJS**

#### Real-time Notifications:
- Thông báo ngay lập tức khi có tương tác
- WebSocket connection để push notifications

### 4.5. Discovery và Search

- **Search functionality**: Tìm kiếm bài viết, người dùng
- **Search history**: Lưu lịch sử tìm kiếm
- **Topic-based discovery**: Khám phá theo chủ đề
- **User discovery**: Tìm kiếm và gợi ý người dùng

### 4.6. Content Moderation (Kiểm duyệt nội dung)

- **Report system**: Báo cáo bài viết/bình luận vi phạm
- **Toxic content detection**: Phát hiện nội dung độc hại (toxic_score)
- **Admin moderation tools**: Công cụ kiểm duyệt cho admin
- **Soft delete**: Xóa mềm để giữ lại dữ liệu cho điều tra

### 4.7. Media Handling

- **Cloudinary integration**: Tích hợp dịch vụ lưu trữ media
- **Avatar và cover images**: Ảnh đại diện và ảnh bìa
- **Post multimedia**: Ảnh và video trong bài viết
- **CDN delivery**: Phân phối nhanh qua CDN
- **Max file size**: 50MB

### 4.8. Admin Features

- **System metrics dashboard**: Thống kê hệ thống
- **RBAC management UI**: Quản lý roles và permissions
- **AI model dashboard**: Theo dõi hiệu suất model AI
- **User management**: Quản lý người dùng

---

## 5. CƠ SỞ DỮ LIỆU

### 5.1. Database Schema

Hệ thống sử dụng **PostgreSQL** với **23 tables** được tổ chức theo các nhóm:

#### Core Tables (Bảng cốt lõi):
- `users`: Thông tin đăng nhập và xác thực
- `profiles`: Thông tin profile người dùng
- `roles`: Các vai trò trong hệ thống
- `permissions`: Các quyền hạn cụ thể
- `user_roles`: Mapping user-role (many-to-many)
- `role_permissions`: Mapping role-permission (many-to-many)

#### Content Tables (Bảng nội dung):
- `posts`: Bài viết
- `post_reactions`: Reactions cho bài viết
- `post_topics`: Mapping post-topic
- `comments`: Bình luận
- `comment_reactions`: Reactions cho bình luận
- `topics`: Chủ đề/danh mục
- `user_topics`: Chủ đề quan tâm của user

#### Social Tables (Bảng mạng xã hội):
- `follows`: Quan hệ theo dõi
- `bookmarks`: Bài viết đã lưu
- `user_blocks`: Người dùng bị chặn
- `user_interactions`: Lịch sử tương tác (cho AI)

#### Messaging Tables (Bảng nhắn tin):
- `conversations`: Cuộc hội thoại
- `messages`: Tin nhắn

#### System Tables (Bảng hệ thống):
- `notifications`: Thông báo
- `search_history`: Lịch sử tìm kiếm
- `reports`: Báo cáo vi phạm
- `refresh_tokens`: Token để refresh JWT

### 5.2. Database Migration

- Sử dụng **Flyway** cho version control database schema
- Migration files trong `src/main/resources/db/migration/`
- Tự động chạy khi khởi động ứng dụng
- Đảm bảo consistency giữa các môi trường

### 5.3. Redis Caching Strategy

Redis được sử dụng cho nhiều mục đích:

1. **OTP Storage**: Lưu OTP với TTL 5 phút
2. **Feed Cache**: Cache feed đã xếp hạng (TTL 10 phút)
3. **Feature Cache**: Cache features đã tính toán (TTL 10 phút)
4. **Share Count Deltas**: Tạm lưu delta để batch update

---

## 6. HỆ THỐNG AI

### 6.1. Training Pipeline (Offline)

#### Bước 1: Data Ingestion
- Đọc dữ liệu Reddit từ file .zst (compressed archives)
- Bao gồm submissions và comments
- Preprocessing và cleaning data

#### Bước 2: Feature Engineering
- Tính toán 19 features từ raw data
- Feature categories: post, author, interaction

#### Bước 3: Preprocessing
- **Outlier capping**: Giới hạn outliers ở 99th percentile
- **Log transforms**: Transform các features skewed
- **Normalization**: Chuẩn hóa features

#### Bước 4: Training
- Algorithm: **Gradient Boosting Regressor** (scikit-learn)
- Early stopping để tránh overfitting
- Hyperparameter tuning

#### Bước 5: Evaluation
- Metrics: RMSE, MAE, NDCG@10
- Validation trên test set

#### Bước 6: Export
- Export model sang **JSON format** (không dùng pickle)
- Custom serialization cho tree structure
- Lưu tại `backend/src/main/resources/ai/lightgbm-ranking-model.json`

### 6.2. Inference Pipeline (Online)

#### Architecture:
```
Backend → FastAPI Service → Feature Vectorizer → Tree Model Scorer → Scores → Backend
```

#### Flow:
1. Backend gửi POST request đến `/api/ranking/predict` với candidate features
2. **FeatureVectorizer** áp dụng transformations giống training
3. **TreeModelScorer** traverse decision trees để tính scores
4. Trả về relevance scores cho backend
5. Backend sắp xếp và cache kết quả

#### Model Format:
- Custom JSON serialization của tree structure
- Không phụ thuộc vào pickle (tránh security issues)
- Portable và dễ debug

### 6.3. AI Toggle và Fallback

- **AI Toggle**: Có thể bật/tắt AI ranking qua config
- **Fallback Ranking**: Khi AI disabled, sử dụng deterministic ranking:
  - Sắp xếp theo hot_score
  - Kết hợp upvote_ratio và recency
- **Graceful Degradation**: Hệ thống vẫn hoạt động khi AI service down

---

## 7. TRIỂN KHAI (DEPLOYMENT)

### 7.1. Docker Compose Setup

Hệ thống sử dụng Docker Compose với 3 configurations:

1. **docker-compose.yaml**: Base configuration
2. **docker-compose.dev.yaml**: Development environment
3. **docker-compose.prod.yaml**: Production environment

### 7.2. Services

#### Backend Service:
- Image: Custom Dockerfile với Maven build
- Port: 8080
- Dependencies: postgres, redis
- Environment: Configurable via .env

#### Frontend Service:
- Image: Custom Dockerfile với Vite build
- Port: 5173 (dev), 80 (prod)
- Dependencies: backend

#### PostgreSQL:
- Image: postgres:16
- Port: 5432
- Persistent volume cho data

#### Redis:
- Image: redis:7-alpine
- Port: 6379
- In-memory caching

#### AI Pipeline:
- Image: Custom Dockerfile với Python
- Port: 8000
- Dependencies: None (standalone service)

#### pgAdmin (Optional):
- Image: dpage/pgadmin4
- Port: 5050
- Web UI cho quản lý database

### 7.3. Access Points

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Scalar API Docs**: http://localhost:8080/scalar
- **AI Service**: http://localhost:8000
- **pgAdmin**: http://localhost:5050

---

## 8. CÁC QUYẾT ĐỊNH THIẾT KẾ QUAN TRỌNG

### 8.1. Clean Architecture
**Lý do**: Tăng testability, maintainability, và flexibility. Dễ dàng thay đổi implementation mà không ảnh hưởng business logic.

### 8.2. Permission-based Authorization
**Lý do**: Linh hoạt hơn role-only RBAC. Có thể gán quyền chi tiết cho từng action, dễ dàng mở rộng.

### 8.3. JWT với Database Validation
**Lý do**: Cân bằng giữa performance và security. Có thể revoke token ngay lập tức khi cần (không hoàn toàn stateless).

### 8.4. Redis Caching Strategy
**Lý do**: Tối ưu performance cho feed và features. Giảm tải cho database và AI service. TTL ngắn để đảm bảo freshness.

### 8.5. AI Toggle với Fallback
**Lý do**: Graceful degradation khi AI service không khả dụng. Hệ thống vẫn hoạt động với ranking đơn giản hơn.

### 8.6. WebSocket cho Real-time
**Lý do**: UX tốt hơn polling. Giảm latency và server load cho chat và notifications.

### 8.7. Cloudinary cho Media
**Lý do**: Offload storage và CDN. Không cần quản lý file storage và image processing. Scalable và reliable.

### 8.8. Flyway Migrations
**Lý do**: Version control cho database schema. Đảm bảo consistency giữa các môi trường. Dễ dàng rollback khi cần.

---

## 9. KẾT LUẬN

**Social Pulse** là một nền tảng mạng xã hội **production-ready** với các đặc điểm nổi bật:

### Điểm mạnh:
✅ **Kiến trúc hiện đại**: Clean Architecture, modular monolith, separation of concerns  
✅ **AI-powered**: Machine learning cho personalized feed ranking  
✅ **Full-stack**: React + Spring Boot + Python, công nghệ phổ biến và mạnh mẽ  
✅ **Real-time**: WebSocket cho chat và notifications  
✅ **Bảo mật**: JWT authentication, RBAC, permission-based authorization  
✅ **Performance**: Redis caching, optimized queries, efficient AI inference  
✅ **Scalability**: Docker-based deployment, microservices-ready architecture  
✅ **Maintainability**: Clean code, comprehensive documentation, test coverage  

### Công nghệ nổi bật:
- **Backend**: Java 21, Spring Boot 4, PostgreSQL, Redis
- **Frontend**: React 19, TypeScript, Tailwind CSS
- **AI**: Python, FastAPI, scikit-learn, Gradient Boosting

### Tính năng đặc trưng:
- **Personalized Feed**: AI ranking với 19 features
- **Real-time Chat**: WebSocket-based messaging
- **Content Moderation**: Report system và toxic detection
- **RBAC**: Flexible permission-based authorization

### Phạm vi ứng dụng:
Dự án này phù hợp cho:
- Nền tảng mạng xã hội quy mô vừa và lớn
- Ứng dụng cần personalization và AI
- Hệ thống yêu cầu real-time features
- Môi trường enterprise với yêu cầu bảo mật cao

Social Pulse thể hiện **kỹ thuật phần mềm tiên tiến** với clean architecture, ML integration, và các design patterns có khả năng mở rộng, phù hợp cho một ứng dụng mạng xã hội hiện đại.
