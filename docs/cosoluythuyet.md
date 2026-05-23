# CƠ SỞ LÝ THUYẾT

## 1. Ý TƯỞNG

### 1.1. Bối cảnh và Động lực

#### 1.1.1. Sự phát triển của mạng xã hội

Mạng xã hội đã trở thành một phần không thể thiếu trong cuộc sống hiện đại. Theo thống kê, tính đến năm 2025, có hơn 5 tỷ người dùng mạng xã hội trên toàn cầu, chiếm khoảng 62% dân số thế giới. Các nền tảng như Facebook, Twitter (X), Instagram, và TikTok đã thay đổi cách con người giao tiếp, chia sẻ thông tin, và tiêu thụ nội dung.

Tuy nhiên, sự phát triển nhanh chóng này cũng đặt ra nhiều thách thức:

**Information Overload (Quá tải thông tin):**
- Người dùng trung bình tiếp xúc với hàng nghìn bài viết mỗi ngày
- Khó khăn trong việc tìm kiếm nội dung có giá trị và phù hợp
- Thời gian lướt mạng xã hội tăng nhưng chất lượng trải nghiệm giảm

**Filter Bubble (Bong bóng lọc):**
- Thuật toán đơn giản tạo ra "echo chambers" - người dùng chỉ thấy nội dung tương tự
- Hạn chế sự đa dạng quan điểm và khám phá nội dung mới
- Ảnh hưởng tiêu cực đến sự phát triển tư duy phản biện

**Engagement Manipulation:**
- Nhiều nền tảng tối ưu hóa cho "engagement" thay vì chất lượng
- Nội dung gây tranh cãi, clickbait được ưu tiên
- Ảnh hưởng đến sức khỏe tinh thần người dùng

#### 1.1.2. Nhu cầu cá nhân hóa thông minh

Để giải quyết các vấn đề trên, cần có một hệ thống cá nhân hóa nội dung **thông minh và có trách nhiệm**:

**Personalization (Cá nhân hóa):**
- Hiểu sở thích và hành vi của từng người dùng
- Đề xuất nội dung phù hợp với nhu cầu cá nhân
- Cân bằng giữa nội dung quen thuộc và nội dung mới

**Quality over Quantity:**
- Ưu tiên nội dung chất lượng cao
- Giảm thiểu spam và nội dung độc hại
- Tăng giá trị thông tin người dùng nhận được

**Transparency và Control:**
- Người dùng hiểu tại sao họ thấy nội dung nào đó
- Có khả năng điều chỉnh thuật toán theo ý muốn
- Kiểm soát dữ liệu cá nhân và quyền riêng tư

### 1.2. Vai trò của AI trong Mạng xã hội

#### 1.2.1. Machine Learning cho Content Ranking

Machine Learning (ML) đã chứng minh hiệu quả trong việc cá nhân hóa nội dung:

**Collaborative Filtering:**
- Dựa trên hành vi của người dùng tương tự
- "Người dùng A và B có sở thích giống nhau, nên A có thể thích nội dung mà B đã tương tác"
- Ưu điểm: Không cần hiểu nội dung, chỉ cần patterns
- Nhược điểm: Cold start problem, filter bubble

**Content-Based Filtering:**
- Phân tích đặc điểm của nội dung
- Đề xuất nội dung tương tự với những gì người dùng đã thích
- Ưu điểm: Không phụ thuộc vào người dùng khác
- Nhược điểm: Hạn chế khám phá nội dung mới

**Hybrid Approaches:**
- Kết hợp nhiều phương pháp
- Sử dụng features từ cả nội dung, người dùng, và tương tác
- LightGBM, Gradient Boosting, Neural Networks cho ranking
- Cân bằng giữa exploitation và exploration

#### 1.2.2. Feature Engineering cho Social Media

Để xếp hạng nội dung hiệu quả, cần trích xuất các features có ý nghĩa:

**Post Features (Đặc trưng bài viết):**
- **Temporal features**: Thời gian đăng, độ "tươi" của nội dung
- **Engagement features**: Số lượt thích, bình luận, chia sẻ
- **Content features**: Độ dài, có multimedia, chủ đề
- **Quality signals**: Upvote ratio, toxic score

**Author Features (Đặc trưng tác giả):**
- **Reputation**: Thâm niên, số lượng bài viết
- **Engagement rate**: Tỷ lệ tương tác trung bình
- **Expertise**: Chuyên môn trong các chủ đề cụ thể

**User-Item Interaction Features:**
- **Affinity score**: Mức độ thân thiết với tác giả
- **Historical interactions**: Lịch sử tương tác với nội dung tương tự
- **Recency**: Thời gian từ lần tương tác cuối

**Contextual Features:**
- **Time of day**: Thời điểm trong ngày
- **Device**: Thiết bị đang sử dụng
- **Location**: Vị trí địa lý (nếu có)

### 1.3. Ý tưởng Social Pulse

#### 1.3.1. Tầm nhìn

**Social Pulse** được thiết kế với tầm nhìn:

> "Tạo ra một nền tảng mạng xã hội nơi mỗi người dùng nhận được nội dung có giá trị nhất với họ, thông qua sự kết hợp giữa trí tuệ nhân tạo và thiết kế có trách nhiệm."

**Mục tiêu cốt lõi:**
1. **Personalization**: Mỗi người dùng có trải nghiệm riêng biệt
2. **Quality**: Ưu tiên nội dung chất lượng cao
3. **Discovery**: Giúp người dùng khám phá nội dung và người dùng mới
4. **Transparency**: Rõ ràng về cách thuật toán hoạt động
5. **Privacy**: Bảo vệ dữ liệu và quyền riêng tư người dùng

#### 1.3.2. Điểm khác biệt và Hướng tiếp cận

**1. AI-Powered Feed với Fallback Mechanism:**
- Sử dụng ML để xếp hạng nội dung (đã implement)
- Có fallback ranking khi AI không khả dụng (đã implement)
- Đảm bảo hệ thống luôn hoạt động ổn định
- **Hạn chế**: Model được train offline trên Reddit data, chưa có online learning

**2. Feature-Rich Ranking:**
- Sử dụng 11 đặc trưng cốt lõi từ nhiều nguồn (sau khi loại bỏ các đặc trưng engagement snapshot nhằm tránh rò rỉ dữ liệu - data leakage)
- Kết hợp post, author, và interaction features
- **Hạn chế**: Một số features như affinity_score còn đơn giản, chưa tối ưu

**3. Multi-Source Candidate Selection:**
- Không chỉ từ người theo dõi (đã implement)
- Bao gồm popular, recent, và random posts
- **Hạn chế**: Chưa có diversity optimization algorithm, chỉ là random sampling đơn giản

**4. Configurable System:**
- Admin có thể bật/tắt AI ranking qua config (đã implement)
- Admin có dashboard để monitor AI performance (đã implement cơ bản)
- **Chưa có**: User-level control (người dùng chưa thể tự điều chỉnh thuật toán)
- **Chưa có**: Explainability (chưa giải thích tại sao user thấy post nào đó)

**5. Clean Architecture:**
- Dễ dàng thay đổi và mở rộng (đã implement)
- Testable và maintainable
- Separation of concerns rõ ràng

#### 1.3.3. Hướng tiếp cận cho các vấn đề

**Information Overload:**

*Đã thực hiện:*
- AI ranking để lọc và xếp hạng nội dung theo mức độ phù hợp
- Feed caching (Redis, TTL 10 phút) để tăng tốc độ load
- Pagination cơ bản (page/size parameters)

*Chưa thực hiện:*
- Chưa có personalized notification filtering
- Chưa có "read later" hoặc content organization features
- Chưa có user feedback mechanism để cải thiện ranking

*Đánh giá:* Giảm được một phần information overload thông qua AI ranking, nhưng chưa giải quyết triệt để.

---

**Filter Bubble:**

*Đã thực hiện:*
- Random posts trong candidate selection để tăng diversity
- Multi-source candidates (following, popular, recent, random)

*Chưa thực hiện:*
- Chưa có exploration/exploitation balance algorithm thực sự (chỉ là random sampling)
- Chưa có diversity optimization trong ranking
- Topic-based discovery còn cơ bản, chưa có recommendation algorithm
- Chưa có "serendipity" features để expose users đến nội dung bất ngờ

*Đánh giá:* Có cố gắng giảm filter bubble nhưng chưa có giải pháp toàn diện. Random sampling là approach đơn giản nhất.

---

**Engagement Manipulation:**

*Đã thực hiện:*
- Upvote/downvote system để người dùng có tiếng nói
- Có toxic_score field trong database (chuẩn bị cho content moderation)

*Chưa thực hiện:*
- Toxic score chưa được tính toán tự động (field tồn tại nhưng chưa có model)
- Chưa có content quality scoring ngoài engagement metrics
- Mặc dù live counters (upvote_count, comment_count, vv.) được dùng khi phục vụ (serving time), chúng đã bị loại hoàn toàn khỏi quá trình huấn luyện ngoại tuyến (offline training) để tránh rò rỉ dữ liệu (target leakage) từ dataset Reddit của Pushshift, buộc model học từ các thuộc tính cấu trúc (structural features) và thuộc tính tác giả (rolling snapshot).
- Chưa có mechanism để penalize clickbait hoặc low-quality viral content

*Đánh giá:* Đã giải quyết triệt để rò rỉ dữ liệu bằng cách loại bỏ các đặc trưng đếm tích lũy trong quá trình huấn luyện, tập trung vào đặc trưng cấu trúc bài viết và uy tín tác giả thực tế.

---

**Privacy Concerns:**

*Đã thực hiện:*
- JWT authentication với refresh token rotation
- Permission-based authorization (RBAC)
- Password hashing với BCrypt (cost 12)
- HTTPS enforcement (trong production config)

*Chưa thực hiện:*
- Chưa có data minimization policy rõ ràng (vẫn collect nhiều data)
- Chưa có user data export/deletion features (GDPR compliance)
- Chưa có privacy dashboard cho users
- Chưa có granular privacy controls (ai có thể thấy gì)
- Chưa có audit logs cho data access

*Đánh giá:* Bảo mật authentication/authorization tốt, nhưng privacy controls và data governance còn thiếu nhiều.

---

**Tổng kết:**

Social Pulse đã implement một số giải pháp cơ bản cho các vấn đề của mạng xã hội hiện đại, đặc biệt là:
- ✅ AI-powered personalized ranking (core feature)
- ✅ Clean architecture và security fundamentals
- ⚠️ Diversity và exploration (approach đơn giản)
- ❌ Content quality và toxic detection (planned, chưa implement)
- ❌ Privacy controls và transparency (thiếu nhiều)

Đây là một **proof-of-concept** thể hiện khả năng áp dụng ML cho social media ranking, chứ chưa phải giải pháp hoàn chỉnh cho tất cả các vấn đề đã nêu.

### 1.4. Phạm vi và Giới hạn

#### 1.4.1. Phạm vi dự án

**Trong phạm vi:**
- Xây dựng nền tảng mạng xã hội cơ bản với đầy đủ tính năng
- Implement AI-powered feed ranking
- Real-time chat và notifications
- Content moderation cơ bản
- Admin dashboard

**Ngoài phạm vi:**
- Video streaming (chỉ hỗ trợ upload video)
- Live streaming
- E-commerce integration
- Advanced analytics và reporting
- Mobile native apps (chỉ có web responsive)

#### 1.4.2. Giới hạn kỹ thuật

**AI Model:**
- Sử dụng LightGBM / Gradient Boosting (không phải Deep Learning)
- Training trên Reddit data (không phải data thực của platform)
- Chưa có online learning (model không tự update)

**Scalability:**
- Thiết kế cho quy mô vừa (< 100K users)
- Chưa có distributed caching
- Chưa có CDN cho static assets

**Real-time:**
- WebSocket cho chat (không phải video call)
- Notifications đơn giản (không có push notifications cho mobile)


### 1.5. Yêu cầu Hệ thống (System Requirements)

#### 1.5.1. Yêu cầu Chức năng (Functional Requirements)

Hệ thống phân chia chi tiết các quyền lợi và chức năng cho 3 nhóm đối tượng người dùng chính:

**1. Khách viếng thăm (Guest):**
- **Đăng ký tài khoản:** Cho phép tạo tài khoản mới với thông tin email, mật khẩu, và bắt buộc xác thực qua OTP gửi đến email.
- **Đăng nhập / Đăng xuất:** Xác thực người dùng bằng JWT Token. Hỗ trợ cơ chế tự động khóa tài khoản tạm thời nếu đăng nhập sai nhiều lần.
- **Khôi phục mật khẩu:** Cho phép đặt lại mật khẩu an toàn bằng cách gửi mã OTP xác nhận qua email.
- **Xem bài viết thịnh hành (Trending Feed):** Cho phép xem các bài viết có xu hướng nổi bật công khai sắp xếp theo tần suất xuất hiện và hashtag phổ biến mà không cần đăng nhập.

**2. Thành viên (Member):**
- **Quản lý nội dung cá nhân:** Tạo, sửa, và xóa bài viết của chính mình (chấp nhận đính kèm hình ảnh/video qua Cloudinary CDN).
- **Chia sẻ bài viết (Share Post):** Cho phép chia sẻ lại bài viết công khai của thành viên khác dưới dạng liên kết kế thừa (parent_post_id).
- **Tương tác phản hồi (Reactions):** Bày tỏ thái độ Upvote/Downvote đối với bài viết hoặc bình luận.
- **Bình luận đa cấp (Comments Thread):** Tạo bình luận và trả lời bình luận (replies) theo cấu trúc cây lồng nhau không giới hạn độ sâu.
- **Nhắn tin tức thời (Real-time Chat):** Chat 1-1 trực tiếp với thành viên khác, lưu trữ lịch sử tin nhắn và đếm số lượng tin nhắn chưa đọc.
- **Hệ thống Theo dõi (Follow System):** Theo dõi hoặc bỏ theo dõi người dùng khác để cá nhân hóa nguồn cấp tin.
- **Hệ thống Chặn (Block System):** Chặn người dùng khác nhằm bảo vệ không gian riêng tư. Việc chặn sẽ tự động hủy liên kết follow giữa hai bên.
- **Lưu trữ bài viết (Bookmarks):** Lưu các bài viết ưa thích vào danh sách bookmark cá nhân.
- **Tìm kiếm & Khám phá:** Tìm kiếm bài viết bằng từ khóa, hashtag hoặc tìm kiếm thông tin người dùng khác.

**3. Quản trị viên (Admin):**
- **Quản lý tài khoản:** Khóa (Ban) hoặc kích hoạt lại (Unban) tài khoản của thành viên vi phạm. Gán hoặc thu hồi vai trò đặc quyền (RBAC).
- **Kiểm duyệt nội dung (Content Moderation):** Duyệt danh sách bài viết/bình luận bị thành viên báo cáo (Reports). Ẩn hoặc xóa bỏ nội dung chứa mã độc hại hoặc có điểm độc hại (toxic_score) vượt ngưỡng.
- **Theo dõi chỉ số hệ thống (Metrics):** Xem bảng thống kê số lượng người dùng mới, số bài viết đăng tải, lượng bài viết bị báo cáo/độc hại theo các khoảng thời gian và xuất báo cáo định dạng CSV.

#### 1.5.2. Yêu cầu Phi chức năng (Non-functional Requirements)

- **Hiệu năng (Performance):**
  - Thời gian phản hồi API lấy bảng tin cá nhân hóa (Feed API) phải dưới 500ms đối với các lượt truy cập trúng cache (Redis cache hit).
  - Tốc độ suy luận của mô hình LightGBM trên FastAPI server phải dưới 100ms cho mỗi lượt tính toán xếp hạng vector.
- **Bảo mật (Security):**
  - Mật khẩu người dùng bắt buộc mã hóa một chiều bằng thuật toán BCrypt với độ muối (work factor) là 12.
  - Sử dụng cơ chế xác thực không trạng thái (stateless JWT) nhưng kết hợp lưu trữ whitelist/blacklist phiên đăng nhập trong Redis để hỗ trợ thu hồi token tức thì khi đổi mật khẩu hoặc đăng xuất.
  - Phân quyền nghiêm ngặt tới cấp độ phương thức (Method-level security) và kiểm tra tính sở hữu tài nguyên (Ownership check) tại tầng nghiệp vụ.
- **Độ tin cậy & Sẵn sàng (Reliability & Availability):**
  - Hệ thống phải duy trì cơ chế "AI Toggle" và "Graceful Degradation". Nếu AI pipeline ngoại tuyến (FastAPI server mất kết nối), hệ thống tự động hạ cấp xuống cơ chế xếp hạng deterministic dựa trên live counters và thời gian thực để người dùng không bị gián đoạn trải nghiệm.
- **Khả năng mở rộng (Scalability):**
  - Kiến trúc backend tổ chức theo hướng Modular Monolith, chia ranh giới rõ ràng giữa 17 module để có thể tách thành các microservices độc lập dễ dàng khi quy mô người dùng tăng lên.
  - Tích hợp Redis làm bộ đệm trung gian cho các chỉ số đếm delta (ví dụ share_count delta) để giảm thiểu số lượng truy vấn ghi dồn dập vào cơ sở dữ liệu PostgreSQL.

---

### 1.6. Wireframes & Phác thảo Giao diện (UI Wireframes)

Để hình dung rõ luồng tương tác và giao diện người dùng của Social Pulse, dưới đây là các bản phác thảo cấu trúc và hình ảnh wireframe thực tế:

#### 1.6.1. Wireframe Giao diện Bảng tin cá nhân hóa (Personalized Feed UI)

Giao diện trang chủ được chia làm 3 cột chính chuẩn phong cách mạng xã hội hiện đại:
- **Cột Trái (Sidebar):** Logo, các tab hướng điều hướng nhanh (Trang chủ, Khám phá, Cá nhân hóa - For You, Tin nhắn, Đánh dấu, Cấu hình) và thẻ hồ sơ người dùng đăng nhập.
- **Cột Giữa (Main Feed):** Khung tạo bài viết nhanh ở trên cùng và danh sách các bài viết đã được xếp hạng thông minh bởi mô hình LightGBM ở dưới. Mỗi thẻ bài viết hiển thị tiêu đề, nội dung rút gọn, hình ảnh/video đi kèm, các tag chủ đề (Topics) và các nút tương tác (Upvote/Downvote, Bình luận, Chia sẻ).
- **Cột Phải (Widgets):** Các mục thông tin mở rộng bao gồm Hashtag thịnh hành (#TrendingHashtags) và danh sách bạn bè đang online để kết nối nhanh.

![Personalized Feed Wireframe Mockup](file:///home/phuquydam/Documents/Social-Pulse/docs/images/feed_wireframe.png)

*Hình 1.1: Wireframe thiết kế giao diện trang chủ Bảng tin cá nhân hóa (For You Feed)*

#### 1.6.2. Phác thảo Cấu trúc Màn hình Chat (Real-time Chat UI)

```
+-------------------------------------------------------------------------+
| [Search Chat]       | Chat: Sarah Chen                                  |
+---------------------+---------------------------------------------------+
|                     | [Sarah Chen] (Active now)                         |
| * Sarah Chen (2m)   |                                                   |
|   "Hey, how is..."  |        [Sarah] Hi! Did you see the new AI post?   |
|                     | 10:30                                             |
| * Mark Lee (1h)     |                                                   |
|   "Let's meet tomorrow"     |  You: Not yet, let me check my For You feed.  |
|                     | 10:31 [Delivered]                                 |
| * Chloe B. (1d)     |                                                   |
|   "Thanks for the link"     |                                           |
|                     |                                                   |
|                     |                                                   |
+---------------------+---------------------------------------------------+
|                     | [ Type your message here...                  ] [>]|
+-------------------------------------------------------------------------+
```

#### 1.6.3. Phác thảo Cấu trúc Bảng quản trị (Admin Dashboard UI)

```
+-------------------------------------------------------------------------+
| [ADMIN PORTAL]                                      Welcome, Admin Alex |
+-------------------------------------------------------------------------+
| Dashboard           | SYSTEM METRICS OVERVIEW (Last 7 Days)             |
|                     |                                                   |
| User Management     |  +-------------+  +-------------+  +-------------+ |
|                     |  | Total Users |  | Total Posts |  | Toxic Posts | |
| Post Moderation     |  |   12,450    |  |   84,120    |  |     320     | |
|                     |  +-------------+  +-------------+  +-------------+ |
| AI Model Settings   |                                                   |
|                     |  AI Ranking Backend: [X] Enable (LightGBM)        |
| Reports Queue       |  [Export Metrics CSV Report]                      |
|                     |                                                   |
+---------------------+---------------------------------------------------+
```

---

## 2. KỸ THUẬT VÀ CÔNG NGHỆ SỬ DỤNG


### 2.1. Kiến trúc Phần mềm

#### 2.1.1. Clean Architecture (Hexagonal Architecture)

**Khái niệm:**
Clean Architecture, được đề xuất bởi Robert C. Martin (Uncle Bob), là một mô hình kiến trúc phần mềm nhấn mạnh vào sự phân tách các mối quan tâm (separation of concerns) và độc lập của business logic với các chi tiết kỹ thuật.

**Nguyên tắc cốt lõi:**

1. **Dependency Rule (Quy tắc phụ thuộc):**
   - Dependencies chỉ được trỏ vào trong (inward)
   - Inner layers không biết gì về outer layers
   - Business logic không phụ thuộc vào framework, UI, database

2. **Layers (Các lớp):**
   ```
   Domain (Core) ← Application ← Infrastructure ← Adapter
   ```
   - **Domain**: Entities, business rules, repository interfaces
   - **Application**: Use cases, business logic orchestration
   - **Infrastructure**: Database, external services implementations
   - **Adapter**: Controllers, presenters, HTTP handlers

**Lợi ích trong Social Pulse:**
- **Testability**: Có thể test business logic mà không cần database thật
- **Flexibility**: Dễ dàng thay đổi database từ PostgreSQL sang MongoDB
- **Maintainability**: Code rõ ràng, dễ hiểu, dễ sửa
- **Independence**: Business logic không bị ràng buộc với Spring Boot

**Ví dụ trong dự án:**
```
com.socialpulse.app.post/
├── adapter/web/          # REST controllers
│   └── PostController.java
├── application/service/  # Business logic
│   └── PostService.java
├── domain/model/         # Core entities
│   ├── Post.java
│   └── PostRepository.java (interface)
└── infrastructure/
    └── persistence/      # JPA implementation
        └── JpaPostRepository.java
```

#### 2.1.2. Modular Monolith

**Khái niệm:**
Modular Monolith là kiến trúc kết hợp giữa monolith và microservices - một ứng dụng đơn nhưng được tổ chức thành các modules độc lập.

**Đặc điểm:**
- Mỗi module có boundaries rõ ràng
- Modules giao tiếp qua interfaces được định nghĩa rõ
- Có thể tách thành microservices sau này nếu cần

**Lợi ích:**
- **Simplicity**: Dễ deploy và debug hơn microservices
- **Performance**: Không có network overhead
- **Consistency**: Dễ dàng maintain data consistency
- **Evolution**: Có thể migrate sang microservices từng phần

**17 Modules trong Social Pulse:**
auth, user, post, comment, feed, follow, bookmark, block, chat, notification, discovery, topic, report, admin, realtime, common, security

### 2.2. Backend Technologies

#### 2.2.1. Spring Boot Framework

**Khái niệm:**
Spring Boot là framework Java giúp đơn giản hóa việc xây dựng ứng dụng Spring với convention-over-configuration và embedded server.

**Các tính năng sử dụng:**

**1. Spring MVC (Web Layer):**
- RESTful API với `@RestController`
- Request mapping với `@GetMapping`, `@PostMapping`, etc.
- Request validation với Bean Validation
- Exception handling với `@ControllerAdvice`

**2. Spring Data JPA (Persistence Layer):**
- Repository pattern với `JpaRepository`
- Query methods: `findByUsername`, `findAllByAuthorId`
- Custom queries với `@Query`
- Pagination và sorting built-in

**3. Spring Security (Security Layer):**
- Authentication với JWT
- Authorization với method security (`@PreAuthorize`)
- CORS configuration
- Password encoding với BCrypt

**4. Spring WebSocket (Real-time Layer):**
- STOMP protocol support
- Message broker configuration
- User-specific messaging

**Lợi ích:**
- **Ecosystem**: Tích hợp dễ dàng với nhiều thư viện
- **Production-ready**: Actuator, metrics, health checks
- **Convention**: Giảm configuration code
- **Community**: Cộng đồng lớn, tài liệu phong phú

#### 2.2.2. PostgreSQL Database

**Khái niệm:**
PostgreSQL là hệ quản trị cơ sở dữ liệu quan hệ mã nguồn mở, mạnh mẽ với hỗ trợ ACID đầy đủ.

**Tính năng sử dụng:**

**1. Relational Model:**
- Foreign keys để đảm bảo referential integrity
- Indexes để tối ưu query performance
- Constraints để enforce business rules

**2. Advanced Features:**
- JSONB column type cho flexible data
- Full-text search capabilities
- Window functions cho analytics
- CTEs (Common Table Expressions) cho complex queries

**3. Transactions:**
- ACID compliance
- Isolation levels
- Savepoints

**Lý do chọn PostgreSQL:**
- **Reliability**: ACID compliance, data integrity
- **Performance**: Efficient indexing, query optimization
- **Features**: Rich feature set, extensibility
- **Open Source**: Miễn phí, cộng đồng mạnh

#### 2.2.3. Redis Caching

**Khái niệm:**
Redis là in-memory data store được sử dụng cho caching, session storage, và message broker.

**Use cases trong Social Pulse:**

**1. Feed Caching:**
```
Key: feed:user:{userId}
Value: List of ranked post IDs
TTL: 10 minutes
```
- Giảm tải cho database và AI service
- Tăng tốc độ load feed

**2. Feature Caching:**
```
Key: features:post:{postId}
Value: JSON of 13 features
TTL: 10 minutes
```
- Tránh tính toán lại features
- Consistency trong ranking

**3. OTP Storage:**
```
Key: otp:email:{email}
Value: OTP code
TTL: 5 minutes
```
- Temporary storage cho verification
- Tự động expire

**4. Share Count Deltas:**
- Batch updates để giảm database writes
- Eventually consistent

**Lợi ích:**
- **Speed**: In-memory, microsecond latency
- **Simplicity**: Simple key-value operations
- **TTL**: Automatic expiration
- **Atomic operations**: Thread-safe

### 2.3. Frontend Technologies

#### 2.3.1. React Framework

**Khái niệm:**
React là thư viện JavaScript để xây dựng user interfaces với component-based architecture.

**Tính năng sử dụng:**

**1. Component-Based:**
- Reusable components: Button, Card, Modal
- Composition over inheritance
- Props và state management

**2. Hooks:**
- `useState`: Local state management
- `useEffect`: Side effects, data fetching
- `useContext`: Global state (auth, theme)
- Custom hooks: `useAuth`, `usePost`, `useChat`

**3. React Router:**
- Client-side routing
- Protected routes
- Lazy loading

**Lợi ích:**
- **Performance**: Virtual DOM, efficient updates
- **Developer Experience**: Hot reload, DevTools
- **Ecosystem**: Huge library ecosystem
- **Community**: Largest React community

#### 2.3.2. TypeScript

**Khái niệm:**
TypeScript là superset của JavaScript với static typing.

**Lợi ích:**

**1. Type Safety:**
```typescript
interface Post {
  id: string;
  content: string;
  authorId: string;
  createdAt: Date;
}

function createPost(post: Post): Promise<Post> {
  // TypeScript ensures post has correct shape
}
```

**2. IDE Support:**
- Autocomplete
- Refactoring tools
- Error detection before runtime

**3. Documentation:**
- Types serve as documentation
- Easier to understand code

**4. Maintainability:**
- Catch errors early
- Safer refactoring

#### 2.3.3. Tailwind CSS

**Khái niệm:**
Tailwind CSS là utility-first CSS framework.

**Lợi ích:**
- **Rapid Development**: Không cần viết CSS custom
- **Consistency**: Design system built-in
- **Performance**: Purge unused CSS
- **Responsive**: Mobile-first utilities

### 2.4. Authentication và Authorization

#### 2.4.1. JWT (JSON Web Tokens)

**Khái niệm:**
JWT là standard mở (RFC 7519) để truyền thông tin an toàn giữa các bên dưới dạng JSON object.

**Cấu trúc JWT:**
```
Header.Payload.Signature
```

**1. Header:**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**2. Payload:**
```json
{
  "sub": "user123",
  "username": "john_doe",
  "roles": ["USER"],
  "exp": 1716307200
}
```

**3. Signature:**
```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

**Flow trong Social Pulse:**

1. **Login:**
   - User gửi username/password
   - Server validate và tạo access token (15 min) + refresh token (7 days)
   - Refresh token lưu trong database

2. **Request với Authentication:**
   - Client gửi access token trong header: `Authorization: Bearer <token>`
   - Server verify signature và expiration
   - Extract user info từ payload

3. **Token Refresh:**
   - Khi access token hết hạn, client gửi refresh token
   - Server validate refresh token với database
   - Tạo access token mới và rotate refresh token

**Lợi ích:**
- **Stateless**: Server không cần lưu session
- **Scalable**: Dễ dàng scale horizontal
- **Cross-domain**: Hoạt động với CORS
- **Mobile-friendly**: Dễ sử dụng trong mobile apps

**Security considerations:**
- Refresh token rotation để tránh replay attacks
- Database validation để có thể revoke ngay lập tức
- Short-lived access tokens
- HTTPS only

#### 2.4.2. RBAC (Role-Based Access Control)

**Khái niệm:**
RBAC là phương pháp phân quyền dựa trên vai trò của người dùng trong hệ thống.

**Model trong Social Pulse:**

```
User ←→ Role ←→ Permission
```

**1. Roles:**
- **GUEST**: Chưa đăng nhập, chỉ xem nội dung công khai
- **USER**: Người dùng thường, có thể tạo bài, bình luận, tương tác
- **ADMIN**: Quản trị viên, có quyền quản lý hệ thống

**2. Permissions (ví dụ):**
- `post:create`, `post:edit`, `post:delete`
- `comment:create`, `comment:edit`, `comment:delete`
- `user:ban`, `user:unban`
- `admin:dashboard`, `admin:metrics`

**3. Authorization Checks:**

**Method-level security:**
```java
@PreAuthorize("hasPermission(#postId, 'Post', 'edit')")
public void updatePost(Long postId, PostUpdateDto dto) {
    // Only owner or admin can edit
}
```

**Service-level checks:**
```java
public void deletePost(Long postId, Long userId) {
    Post post = postRepository.findById(postId);
    if (!post.getAuthorId().equals(userId) && !isAdmin(userId)) {
        throw new ForbiddenException();
    }
    // Delete logic
}
```

**Lợi ích:**
- **Flexibility**: Dễ dàng thêm roles và permissions mới
- **Granularity**: Phân quyền chi tiết đến từng action
- **Maintainability**: Centralized authorization logic
- **Auditability**: Dễ dàng audit ai có quyền gì

### 2.5. Machine Learning

#### 2.5.1. LightGBM (Light Gradient Boosting Machine)

**Khái niệm:**
LightGBM là một framework gradient boosting dựa trên cây quyết định (Decision Tree) phát triển bởi Microsoft. Social Pulse sử dụng LightGBM làm mô hình xếp hạng chính nhờ vào hiệu năng vượt trội, tốc độ huấn luyện nhanh và lượng tài nguyên RAM tiêu thụ cực thấp khi suy luận thời gian thực (inference).

**Thuật toán & Điểm cải tiến cốt lõi:**

1. **Leaf-wise (Best-first) Tree Growth:**
   - Khác với hầu hết các thư viện Gradient Boosting thông thường (mọc cây theo chiều ngang - level-wise), LightGBM phát triển cây theo chiều sâu (leaf-wise). Nó chọn node có loss giảm nhiều nhất để phân tách tiếp.
   - Giúp giảm sai số (loss) tốt hơn, tăng độ chính xác nhưng cần kiểm soát `max_depth` nghiêm ngặt để tránh quá khớp (overfitting).

2. **Histogram-based Algorithm:**
   - Nhóm các giá trị đặc trưng liên tục vào các thùng (bins) rời rạc.
   - Giúp giảm đáng kể chi phí tìm điểm phân tách tối ưu trên tập dữ liệu lớn.

3. **Cơ chế Fallback:**
   - Nhằm tối ưu hóa khả năng tương thích và chạy thử nghiệm, hệ thống hỗ trợ cơ chế tự động hạ cấp xuống mô hình `GradientBoostingRegressor` của thư viện `scikit-learn` trên CPU nếu môi trường chạy không cài đặt hoặc không hỗ trợ LightGBM.

**Công thức cập nhật mô hình:**
```
F_m(x) = F_{m-1}(x) + η * h_m(x)
```
Trong đó:
- $F_m(x)$ là dự đoán của ensemble sau $m$ cây.
- $\eta$ là Learning rate (tốc độ học).
- $h_m(x)$ là cây thứ $m$ được huấn luyện trên residual của bước trước.

**Các Hyperparameters quan trọng cấu hình trong dự án:**
- **num_leaves**: Số lượng lá tối đa trong một cây ($2^{max\_depth} - 1$). Giới hạn độ phức tạp của cây để chống overfitting.
- **learning_rate**: Hệ số co hẹp của mỗi cây (thường cấu hình từ $0.01$ đến $0.1$).
- **max_depth**: Chiều sâu tối đa của cây nhằm giới hạn độ sâu leaf-wise.
- **min_child_samples**: Số lượng mẫu tối thiểu cần có tại một node lá (tương đương `min_samples_leaf`).
- **reg_lambda / reg_alpha**: Các tham số chuẩn hóa L1/L2 chống overfitting.
- **early_stopping_rounds**: Dừng sớm quá trình huấn luyện nếu metric đánh giá trên tập validation (RMSE, MAE hoặc NDCG@10) không cải thiện sau số lượt chỉ định.

**Lý do chọn LightGBM:**
- **Inference Speed**: Tốc độ suy luận cực nhanh, phù hợp cho luồng xử lý thời gian thực của feed API.
- **Memory Efficiency**: Tiết kiệm RAM đáng kể nhờ histogram-based binning.
- **Gain-Based Feature Importance**: Cho phép phân tích tầm quan trọng của các đặc trưng đóng góp vào điểm xếp hạng dựa trên tổng lượng Gain tăng lên khi chia tách cây.


#### 2.5.2. Feature Engineering & Ngăn ngừa Rò rỉ Dữ liệu (Data Leakage)

**Khái niệm:**
Feature Engineering là quá trình chuyển đổi dữ liệu thô (raw data) thành các đặc trưng đầu vào có ý nghĩa để tối ưu hóa hiệu năng mô hình LightGBM. Trong Social Pulse, quá trình này được cải tiến để giải quyết triệt để hai bẫy dữ liệu lớn nhất trong các tập dữ liệu cào offline (như Pushshift Reddit): **Rò rỉ mục tiêu (Target Leakage)** và **Rò rỉ thời gian (Temporal Leakage/Feature Skew)**.

**1. Bẫy Rò rỉ Mục tiêu (Target Leakage):**
- **Vấn đề:** Tập dữ liệu Pushshift Reddit cung cấp các số liệu tương tác (`upvote_count`, `downvote_count`, `comment_count`, `share_count`, `view_count`) tại thời điểm cào dữ liệu (thường là nhiều ngày/tháng sau khi bài viết được đăng). Nếu đưa các đặc trưng đếm tích lũy này vào huấn luyện offline, mô hình sẽ học vẹt dựa trên các số liệu tương tác cuối cùng để đoán nhãn (log1p popularity), thay vì học cách xếp hạng từ các yếu tố nội tại của bài viết. Khi đưa vào suy luận thực tế (inference) cho bài viết mới đăng, các số đếm này bằng 0 hoặc rất nhỏ, dẫn đến mô hình hoạt động kém hiệu quả.
- **Giải pháp:** Loại bỏ hoàn toàn 5 đặc trưng đếm tương tác tích lũy này cùng với đặc trưng `popularity` ra khỏi vector huấn luyện offline. Mô hình bị buộc phải học cách xếp hạng từ các thuộc tính cấu trúc nội tại và độ uy tín của tác giả.

**2. Bẫy Rò rỉ Thời gian (Temporal Leakage) & Feature Skew:**
- **Sai lệch tuổi bài viết (post_age_hours):** Trong huấn luyện offline, tuổi của bài viết phải được tính tương đối theo thời điểm truy vấn tương tác ảo của người dùng thay vì tính theo mốc cố định cuối dataset. Điều này được điều chỉnh để khớp chính xác logic tính toán thời gian thực khi đưa vào sản xuất.
- **Sai lệch độ uy tín tác giả (author_engagement_rate):** Việc quét toàn bộ file dữ liệu để tính tỷ lệ tương tác trung bình (`average_popularity`) trước khi train khiến bài viết từ quá khứ (ví dụ: tháng 1) lại được thừa hưởng độ nổi tiếng của tác giả ở tương lai (ví dụ: tháng 12). 
- **Giải pháp:** Sử dụng cơ chế cập nhật trạng thái rolling (**Sequential Rolling Snapshot**). Khi duyệt qua tập dữ liệu theo thứ tự thời gian tuyến tính, số liệu của tác giả chỉ được cập nhật dựa trên những bài viết trước thời điểm bài viết hiện tại được đăng, bảo đảm không có thông tin tương lai nào bị rò rỉ vào đặc trưng huấn luyện.

**Danh sách 11 đặc trưng cốt lõi (Core Feature Schema):**

| STT | Tên Đặc Trưng | Thể Loại | Mô Tả |
|---|---|---|---|
| 1 | `content_length` | Post (Cấu trúc) | Tổng độ dài tiêu đề và nội dung bài viết. |
| 2 | `has_multimedia` | Post (Cấu trúc) | Nhị phân (0 hoặc 1), chỉ ra bài viết có chứa hình ảnh hoặc video. |
| 3 | `is_share_post` | Post (Cấu trúc) | Nhị phân (0 hoặc 1), bài viết là bài chia sẻ (share post) lại từ bài khác. |
| 4 | `post_age_hours` | Post (Thời gian) | Số giờ trôi qua kể từ khi đăng đến lúc truy vấn. |
| 5 | `author_seniority` | Author (Tác giả) | Thâm niên của tác giả (tính bằng năm) kể từ ngày tạo tài khoản. |
| 6 | `author_post_count` | Author (Tác giả) | Tổng số bài viết của tác giả tính đến trước thời điểm hiện tại. |
| 7 | `author_engagement_rate` | Author (Tác giả) | Điểm tương tác trung bình của các bài viết trước đó của tác giả (Snapshot rolling). |
| 8 | `interaction_count_7d` | Interaction (Tương tác) | Số lượt tương tác của người xem với tác giả này trong 7 ngày qua. |
| 9 | `interaction_count_30d` | Interaction (Tương tác) | Số lượt tương tác của người xem với tác giả này trong 30 ngày qua. |
| 10 | `hours_since_last_interaction` | Interaction (Tương tác) | Số giờ kể từ lần tương tác cuối của người xem với tác giả. |
| 11 | `affinity_score` | Interaction (Tương tác) | Điểm thân thiết tính bằng tỷ lệ tương tác với tác giả trên tổng tương tác của người xem. |

**Techniques tiền xử lý toán học (Preprocessing):**

**1. Log Transforms (Biến đổi logarit):**
Áp dụng đối với các đặc trưng tương tác có phân phối lệch (skewed distribution) như `interaction_count_7d` và `interaction_count_30d` để đưa về phân phối chuẩn hơn:
```python
x_transformed = log(x + 1)
```

**2. Outlier Capping (Giới hạn ngoại lai):**
Các đặc trưng có khoảng giá trị quá rộng (`content_length`, `post_age_hours`, `author_seniority`, `author_post_count`, `author_engagement_rate`, `hours_since_last_interaction`) được giới hạn ở phân vị thứ 99 (99th percentile) để hạn chế ảnh hưởng tiêu cực của nhiễu dữ liệu:
```python
capped_value = min(value, percentile_99)
```

**Tầm quan trọng:**
Sự kết hợp giữa ngăn ngừa rò rỉ dữ liệu thông qua cấu trúc 11 đặc trưng và áp dụng các biến đổi phân phối giúp LightGBM đạt NDCG@10 ổn định hơn, mô hình tổng quát hóa tốt hơn khi gặp các dữ liệu mới trong môi trường thực tế.


### 2.6. Real-time Communication

#### 2.6.1. WebSocket Protocol

**Khái niệm:**
WebSocket là protocol cung cấp full-duplex communication channel qua một TCP connection.

**So sánh với HTTP:**

| Feature | HTTP | WebSocket |
|---------|------|-----------|
| Connection | Request-response | Persistent |
| Direction | Client → Server | Bidirectional |
| Overhead | High (headers mỗi request) | Low (handshake một lần) |
| Latency | Higher | Lower |
| Use case | REST APIs | Real-time apps |

**STOMP Protocol:**
STOMP (Simple Text Oriented Messaging Protocol) là protocol layer trên WebSocket:

```
CONNECT
destination: /app/chat
```

```
SEND
destination: /app/chat.sendMessage
content-type: application/json

{"content": "Hello", "conversationId": "123"}
```

```
SUBSCRIBE
destination: /user/queue/messages
```

**Implementation trong Social Pulse:**

**1. Server Configuration:**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
```

**2. Message Controller:**
```java
@MessageMapping("/chat.sendMessage")
@SendToUser("/queue/messages")
public Message sendMessage(Message message) {
    return chatService.saveAndSend(message);
}
```

**3. Client Connection:**
```typescript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
    stompClient.subscribe('/user/queue/messages', (message) => {
        handleNewMessage(JSON.parse(message.body));
    });
});
```

**Lợi ích:**
- **Low Latency**: Tin nhắn được gửi ngay lập tức
- **Efficient**: Không cần polling
- **Scalable**: Một connection cho nhiều messages
- **User Experience**: Real-time, responsive

### 2.7. DevOps và Deployment

#### 2.7.1. Docker Containerization

**Khái niệm:**
Docker là platform để develop, ship, và run applications trong containers - lightweight, standalone packages chứa mọi thứ cần để chạy application.

**Lợi ích:**

**1. Consistency:**
- "Works on my machine" → "Works everywhere"
- Same environment từ dev đến production

**2. Isolation:**
- Mỗi service trong container riêng
- Không conflict dependencies

**3. Portability:**
- Chạy trên bất kỳ platform nào có Docker
- Cloud-agnostic

**4. Scalability:**
- Dễ dàng scale services
- Orchestration với Docker Compose, Kubernetes

**Docker Compose trong Social Pulse:**

```yaml
services:
  backend:
    build: ./backend
    ports: ["8080:8080"]
    depends_on: [postgres, redis]
    
  frontend:
    build: ./frontend
    ports: ["5173:5173"]
    depends_on: [backend]
    
  postgres:
    image: postgres:16
    volumes: [postgres_data:/var/lib/postgresql/data]
    
  redis:
    image: redis:7-alpine
    
  ai_pipeline:
    build: ./ai_pipeline
    ports: ["8000:8000"]
```

**Lợi ích trong dự án:**
- One-command setup: `docker-compose up`
- Consistent environments
- Easy to onboard new developers
- Production-like local development

#### 2.7.2. Database Migration với Flyway

**Khái niệm:**
Flyway là database migration tool giúp version control database schema.

**Cách hoạt động:**

**1. Migration Files:**
```
V1__initial_schema.sql
V2__add_posts_table.sql
V3__add_comments_table.sql
```

**2. Flyway Schema History Table:**
```sql
CREATE TABLE flyway_schema_history (
    installed_rank INT,
    version VARCHAR(50),
    description VARCHAR(200),
    script VARCHAR(1000),
    checksum INT,
    installed_on TIMESTAMP,
    success BOOLEAN
);
```

**3. Migration Process:**
- Flyway checks current version
- Applies pending migrations in order
- Records in history table

**Lợi ích:**
- **Version Control**: Schema changes tracked in Git
- **Reproducibility**: Same migrations in all environments
- **Rollback**: Can rollback to previous versions
- **Team Collaboration**: No schema conflicts

### 2.8. API Design

#### 2.8.1. RESTful API Principles

**Khái niệm:**
REST (Representational State Transfer) là architectural style cho distributed systems.

**Principles:**

**1. Resource-Based:**
```
GET    /api/posts          # Get all posts
GET    /api/posts/{id}     # Get specific post
POST   /api/posts          # Create post
PUT    /api/posts/{id}     # Update post
DELETE /api/posts/{id}     # Delete post
```

**2. HTTP Methods:**
- **GET**: Retrieve (idempotent, safe)
- **POST**: Create (not idempotent)
- **PUT**: Update (idempotent)
- **DELETE**: Delete (idempotent)

**3. Status Codes:**
- **200 OK**: Success
- **201 Created**: Resource created
- **400 Bad Request**: Invalid input
- **401 Unauthorized**: Not authenticated
- **403 Forbidden**: Not authorized
- **404 Not Found**: Resource not found
- **500 Internal Server Error**: Server error

**4. Stateless:**
- Each request contains all information needed
- No server-side session

**5. Pagination:**
```
GET /api/posts?page=0&size=20&sort=createdAt,desc
```

**Best Practices trong Social Pulse:**
- Consistent naming conventions
- Versioning: `/api/v1/posts`
- Error responses with details
- HATEOAS links (optional)
- OpenAPI documentation

### 2.9. Security Best Practices

#### 2.9.1. OWASP Top 10 Mitigations

**1. Injection Prevention:**
- Parameterized queries (JPA)
- Input validation
- Escaping user input

**2. Broken Authentication:**
- Strong password hashing (BCrypt cost 12)
- JWT with short expiration
- Refresh token rotation

**3. Sensitive Data Exposure:**
- HTTPS only
- Passwords never logged
- Secrets in environment variables

**4. XML External Entities (XXE):**
- Use JSON instead of XML
- Disable XML external entity processing

**5. Broken Access Control:**
- Method-level security
- Ownership checks
- Principle of least privilege

**6. Security Misconfiguration:**
- Disable debug mode in production
- Remove default credentials
- Keep dependencies updated

**7. Cross-Site Scripting (XSS):**
- Content Security Policy headers
- Escape user input in frontend
- Sanitize HTML content

**8. Insecure Deserialization:**
- Validate input before deserialization
- Use safe serialization formats (JSON)

**9. Using Components with Known Vulnerabilities:**
- Regular dependency updates
- Automated vulnerability scanning

**10. Insufficient Logging & Monitoring:**
- Log authentication events
- Monitor suspicious activities
- Alerting for security events

#### 2.9.2. Additional Security Measures

**CORS Configuration:**
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowCredentials(true);
        return new CorsFilter(source);
    }
}
```

**Rate Limiting:**
- Prevent brute force attacks
- Limit API calls per user
- Redis-based rate limiting

**Content Security Policy:**
```
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'
```

---

## KẾT LUẬN PHẦN CƠ SỞ LÝ THUYẾT

Phần cơ sở lý thuyết đã trình bày:

**1. Ý tưởng:**
- Bối cảnh và động lực phát triển Social Pulse
- Vai trò của AI trong mạng xã hội hiện đại
- Giải pháp cho các vấn đề information overload, filter bubble
- Tầm nhìn và điểm khác biệt của dự án

**2. Kỹ thuật và Công nghệ:**
- **Kiến trúc**: Clean Architecture, Modular Monolith
- **Backend**: Spring Boot, PostgreSQL, Redis
- **Frontend**: React, TypeScript, Tailwind CSS
- **Security**: JWT, RBAC, OWASP mitigations
- **AI**: LightGBM, Ngăn ngừa Rò rỉ Dữ liệu (Target/Temporal Leakage)
- **Real-time**: WebSocket, STOMP protocol
- **DevOps**: Docker, Flyway migrations
- **API**: RESTful principles, OpenAPI documentation

Các công nghệ và kỹ thuật được lựa chọn dựa trên:
- **Maturity**: Công nghệ đã được chứng minh trong production
- **Community**: Cộng đồng lớn, tài liệu phong phú
- **Performance**: Đáp ứng yêu cầu về tốc độ và scalability
- **Maintainability**: Dễ bảo trì và mở rộng
- **Security**: Đảm bảo an toàn cho người dùng

Sự kết hợp của các công nghệ này tạo nên một nền tảng mạng xã hội hiện đại, an toàn, và có khả năng cá nhân hóa cao thông qua AI.

