# Overall System Architecture (Sơ đồ Kiến trúc Tổng quát Hệ thống)

Sơ đồ này mô tả luồng dữ liệu tổng quát từ phía người dùng cuối (Frontend) qua máy chủ dịch vụ (Backend Spring Boot) đến dịch vụ xếp hạng khuyến nghị (AI Ranking Service FastAPI) và lưu trữ/caching dữ liệu.

```mermaid
flowchart TD
  %% Style definitions
  classDef client fill:#f0fdfa,stroke:#0d9488,stroke-width:2px,color:#0f766e;
  classDef api fill:#f0fdf4,stroke:#16a34a,stroke-width:2px,color:#15803d;
  classDef ai fill:#faf5ff,stroke:#7c3aed,stroke-width:2px,color:#6d28d9;
  classDef data fill:#fff7ed,stroke:#ea580c,stroke-width:2px,color:#c2410c;

  subgraph Client_Layer ["Client Layer (React Frontend)"]
    UI["Web/Mobile UI<br>(React + Vite)"]:::client
    SC["Local State / Cache<br>(Feed State)"]:::client
    Auth["Auth State<br>(JWT Context)"]:::client
    SSE["SSE Client Listener<br>(RealTimeContext)"]:::client
  end

  subgraph API_Layer ["API Layer (Spring Boot Backend)"]
    Gateway["Security Filter<br>(JWT verification)"]:::api
    Controller["FeedController<br>(GET /api/v1/feed)"]:::api
    FeedSvc["GetFeedService<br>(Orchestration)"]:::api
    RankOrch["FeedRankingService<br>(Ranking Orchestrator)"]:::api
    Extract["FeatureExtractionService<br>(Features Builder)"]:::api
    BoostSvc["ScoreBoostService<br>(Business rules & weights)"]:::api
    FallbackSvc["FallbackRankingService<br>(Heuristics / Chronological)"]:::api
    AIClient["AiPipelineRankingClient<br>(Spring RestClient)"]:::api
    SSEStream["SseEmitterRegistry<br>(SSE Event Broadcaster)"]:::api
  end

  subgraph AI_Layer ["AI Inference Layer (FastAPI Service)"]
    FastAPI["FastAPI App<br>(/api/ranking/predict)"]:::ai
    Vec["Inference Vectorizer<br>(Capping, log1p preprocessing)"]:::ai
    LGBM["LightGBM Model Predictor<br>(model.txt + model.json)"]:::ai
  end

  subgraph Data_Layer ["Data & Caching Layer"]
    Postgres[("PostgreSQL DB<br>- Users, Posts, Follows<br>- Impressions & Interactions")]:::data
    Redis[("Redis Cache<br>- Feed Cache: user:feed:id<br>- Seen Posts: user:seen:id")]:::data
  end

  %% Flow lines
  UI -->|1. Request Feed (JWT Token)| Gateway
  Gateway -->|2. Route to| Controller
  Controller -->|3. Call getFeed()| FeedSvc
  
  %% Cache hit/miss flow
  FeedSvc -->|4. Check Feed Cache| Redis
  Redis -.->|Cache Hit: Return Cached Feed| FeedSvc
  
  %% Cache miss flow
  FeedSvc -->|5. Cache Miss: Rank Feed| RankOrch
  RankOrch -->|6. Select Candidate Posts| Postgres
  RankOrch -->|7. Extract Features| Extract
  Extract -->|7a. Retrieve metrics| Postgres
  
  %% FastAPI client call
  RankOrch -->|8. Request Predict Scores| AIClient
  AIClient -->|9. HTTP POST JSON /predict (5s timeout)| FastAPI
  FastAPI -->|10. Preprocess Features| Vec
  Vec -->|11. Predict Score| LGBM
  LGBM -->|12. Return Scores| FastAPI
  FastAPI -->|13. JSON Response| AIClient
  
  %% Fallback flow
  AIClient -.->|Timeout / Error Fallback| FallbackSvc
  FallbackSvc -.->|Compute Popularity/Chronological| RankOrch
  
  %% Re-ranking, Caching, Seen marking & Impressions
  RankOrch -->|14. Re-ranking & Score Boosting| BoostSvc
  BoostSvc -->|15. Cache Ranked Feed| Redis
  BoostSvc -->|16. Return Sorted Feed Items| FeedSvc
  FeedSvc -->|17. Mark Seen| Redis
  FeedSvc -->|18. Log Impression Metadata| Postgres
  FeedSvc -->|19. JSON Response (Ranked Feed)| UI
  
  %% Real-time update flow
  Postgres -.->|New Post Created| Controller
  Controller -->|Evict Cache & Broadcast SSE| SSEStream
  SSEStream -->|SSE Event: 'feed_refresh'| SSE
  SSE -->|Refresh Trigger| UI
```

## 1. Phân Tích Các Tầng (Layers)

*   **Client Layer:** Giao diện người dùng sử dụng React Web, quản lý trạng thái xác thực JWT thông qua Context, lưu trữ danh sách tin tức trong Local State. Lắng nghe cập nhật thời gian thực bằng `EventSource` (SSE client) từ Backend.
*   **API Layer (Spring Boot):** Đóng vai trò là Orchestrator chính của hệ thống. Nhận yêu cầu từ client, xử lý bảo mật JWT, lấy mẫu thô các ứng viên (Candidate Generation), trích xuất các đặc trưng và gọi dịch vụ AI. Áp dụng thêm cơ chế Fallback Ranking (khi AI bị lỗi hoặc quá thời gian phản hồi 5 giây) và Re-ranking (Score Boosting) dựa trên hành vi người dùng trước khi trả về.
*   **AI Layer (FastAPI):** Dịch vụ chấm điểm khuyến nghị siêu nhanh. Sử dụng vectorizer để chuyển hóa dữ liệu thô (capping outlier, log1p tương tác) khớp với Schema lúc huấn luyện, sau đó đưa vào mô hình LightGBM để tính điểm.
*   **Data Layer:** PostgreSQL lưu trữ toàn bộ dữ liệu lâu dài và quan hệ (Người dùng, Bài viết, Bình luận, Tương tác, Impressons). Redis đảm nhận vai trò lưu vết các bài đăng đã xem để lọc bỏ (`user:seen:<id>`), và lưu trữ danh sách bảng tin đã xếp hạng của mỗi user (`user:feed:<id>`) giúp tăng tốc tải trang.

## 2. Các Cơ Chế Đặc Thù

1.  **Seen Posts & Impression Logging (Bộ lọc bài đã xem):** Mỗi khi feed được trả về, danh sách ID bài đăng sẽ được lưu vào Redis Set của user đó với TTL là 7 ngày để tránh hiển thị lại. Đồng thời, toàn bộ vết hiển thị (Impression metadata) được lưu không đồng bộ vào PostgreSQL để hỗ trợ thống kê và huấn luyện lại sau này.
2.  **Fallback Mechanism:** Khi kết nối đến AI Server gặp sự cố (Network Error/Timeout), hệ thống tự động gọi `FallbackRankingService` chấm điểm theo thuật toán Heuristics (độ phổ biến cộng tuyến tính thời gian đăng bài) nhằm đảm bảo hệ thống không bị crash.
3.  **Realtime Feed Refresh:** Khi người dùng tạo hoặc sửa đổi bài viết, backend lập tức thu hồi cache Redis của chính họ (`redis.delete("user:feed:" + userId)`), đồng thời phát sóng sự kiện `feed_refresh` qua Server-Sent Events (SSE). Client nhận sự kiện này sẽ tự động gọi tải lại bảng tin mà không cần người dùng tải lại trang thủ công.
