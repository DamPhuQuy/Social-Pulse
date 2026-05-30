# High-Level Architecture Flow (Sơ đồ Luồng Kiến trúc Cấp cao)

Sơ đồ này biểu diễn mô hình kiến trúc cấp cao (High-Level Block Diagram) của hệ thống **Social Pulse**, chỉ tập trung vào sự tương tác giữa các thành phần chính mà không thể hiện các phần tử nội bộ bên trong mỗi thành phần.

```mermaid
graph TD
  %% Style definitions
  classDef frontend fill:#f0fdfa,stroke:#0d9488,stroke-width:2.5px,color:#0f766e;
  classDef backend fill:#f0fdf4,stroke:#16a34a,stroke-width:2.5px,color:#15803d;
  classDef ai fill:#faf5ff,stroke:#7c3aed,stroke-width:2.5px,color:#6d28d9;
  classDef database fill:#fff7ed,stroke:#ea580c,stroke-width:2.5px,color:#c2410c;
  classDef dataset fill:#f8fafc,stroke:#64748b,stroke-width:2.5px,color:#475569;

  %% Nodes definition
  Frontend["React Frontend<br>(Web / Mobile UI)"]:::frontend
  Backend["Spring Boot Backend<br>(Core REST API & Orchestrator)"]:::backend
  AIService["FastAPI AI Service<br>(LightGBM Inference Engine)"]:::ai
  
  subgraph Data_Stores ["Data Stores"]
    Redis[("Redis Memory Cache<br>(Feed & Seen Posts Cache)")]:::database
    Postgres[("PostgreSQL Database<br>(User, Post & Interaction data)")]:::database
  end

  subgraph Offline_ML ["Offline ML Pipeline"]
    Reddit[("Pushshift Reddit Dataset<br>(Submissions & Comments ZST)")]:::dataset
    Training["ML Training Pipeline<br>(Feature extraction & training)"]:::dataset
  end

  %% Flow and Connections
  Frontend <-->|1. REST API Requests & SSE Updates<br>(Bearer JWT Authentication)| Backend
  
  Backend <-->|2. Cache Check & Write<br>(seen posts & feed cache)| Redis
  Backend <-->|3. Persistence Operations<br>(candidate post retrieval & logs)| Postgres
  Backend -->|4. Request Candidate Ranking<br>(HTTP REST POST /predict)| AIService
  
  Reddit -->|5. Raw Data Source| Training
  Training -->|6. Deploy Serialized Model<br>(model.txt & model.json)| AIService

  style Data_Stores fill:none,stroke:#ea580c,stroke-width:1px,stroke-dasharray: 5 5
  style Offline_ML fill:none,stroke:#64748b,stroke-width:1px,stroke-dasharray: 5 5
```

## Giải Thích Luồng Kiến Trúc Cấp Cao (High-Level Flows)

1.  **Tương tác Client - Server (Frontend ↔ Backend):**
    *   Người dùng tương tác trên Frontend (lướt tin, like, comment, share, đăng bài mới).
    *   Frontend giao tiếp với Spring Boot Backend qua RESTful API, kèm theo mã xác thực JWT.
    *   Backend cập nhật thay đổi và phát tín hiệu tải lại thời gian thực cho Frontend qua Server-Sent Events (SSE).
2.  **Truy xuất & Caching dữ liệu (Backend ↔ Caching/Database):**
    *   Backend lưu vết các bài viết người dùng đã xem vào Redis để tránh lặp lại nội dung.
    *   Bảng tin đã được xếp hạng được lưu trữ ngắn hạn trên Redis giúp tăng tốc tải trang cho các yêu cầu tiếp theo.
    *   PostgreSQL lưu trữ dữ liệu vĩnh viễn bao gồm cấu trúc người dùng, bài đăng, các mối quan hệ follower, và vết hiển thị (impressions).
3.  **Xếp hạng trực tuyến (Backend → AI Service):**
    *   Khi có yêu cầu tải tin mới và bị lỡ cache (cache miss), Backend truy vấn các bài viết ứng viên tiềm năng từ PostgreSQL.
    *   Backend gửi các thông tin bài viết và người dùng sang FastAPI AI Service để tính toán điểm số cá nhân hóa. Dịch vụ AI phản hồi danh sách điểm xếp hạng.
4.  **Quy trình máy học ngoại tuyến (Offline ML Pipeline):**
    *   Mô hình LightGBM được huấn luyện định kỳ offline bằng cách quét và xử lý tập dữ liệu Reddit thô.
    *   Sau khi huấn luyện thành công, cấu trúc cây quyết định (`model.txt`) và các quy tắc tiền xử lý (`model.json`) được tải lên dịch vụ FastAPI để phục vụ dự đoán thời gian thực.
