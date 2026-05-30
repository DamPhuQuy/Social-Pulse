# Social Pulse System Architecture Diagrams (Sơ đồ Kiến trúc Hệ thống)

Chào mừng bạn đến với tài liệu kiến trúc hệ thống của dự án **Social Pulse** - mạng xã hội tích hợp hệ thống gợi ý bài viết cá nhân hóa bằng AI (LightGBM).

Dưới đây là danh sách các sơ đồ kiến trúc được xây dựng bằng MermaidJS, tương thích hiển thị trực tiếp trên trình duyệt Markdown hoặc các công cụ xuất báo cáo (PDF/Word):

## 0. [High-Level Architecture Flow](file:///home/phuquydam/Documents/Social-Pulse/docs/diagram_0_high_level_architecture.md)
*   **Mục tiêu:** Mô tả mô hình kiến trúc cấp cao ở mức vĩ mô (block diagram) giữa các thành phần (Frontend, Backend, AI Service, Redis, Postgres và Offline Training) mà không thể hiện cấu trúc mã nguồn nội bộ.

## 1. [Overall System Architecture](file:///home/phuquydam/Documents/Social-Pulse/docs/diagram_1_system_architecture.md)
*   **Mục tiêu:** Mô tả luồng dữ liệu tổng quát đi từ thiết bị người dùng (Client) qua máy chủ điều phối chính (Spring Boot Backend), gửi yêu cầu trích xuất đặc trưng và tính toán điểm số sang dịch vụ AI (FastAPI LightGBM), lưu/truy xuất bộ nhớ đệm (Redis), ghi nhận lịch sử (PostgreSQL) và trả kết quả về bảng tin.
*   **Luồng đặc trưng:** Fallback khi AI timeout, Seen filter, Real-time update qua Server-Sent Events (SSE).

## 2. [AI Pipeline Architecture](file:///home/phuquydam/Documents/Social-Pulse/docs/diagram_2_ai_pipeline.md)
*   **Mục tiêu:** Trình bày chi tiết toàn bộ chu trình xử lý dữ liệu máy học.
*   **Hai giai đoạn riêng biệt:**
    *   *Offline Training Phase:* Đọc Pushshift Reddit Dataset, lọc Bot/Spam/NSFW (`scanner.py`), tạo mẫu âm ngẫu nhiên khó (`feature_engineering.py`), tiền xử lý (Capping p99, log1p) và huấn luyện LightGBM (`trainer.py`) để sinh ra mô hình `model.txt` / `model.json`.
    *   *Online Inference Phase:* FastAPI nhận ứng viên, nạp cấu hình tiền xử lý, thực hiện vector hóa và chạy mô hình chấm điểm thời gian thực.

## 3. [Backend Spring Boot Architecture](file:///home/phuquydam/Documents/Social-Pulse/docs/diagram_3_backend_architecture.md)
*   **Mục tiêu:** Bản vẽ chi tiết cấu trúc modular backend theo nguyên lý Clean Architecture / Domain-Driven Design (DDD).
*   **Chi tiết các lớp:**
    *   *Presentation/Web Layer:* Nhận API, điều hướng xác thực JWT, phát sóng sự kiện SSE.
    *   *Application Layer:* Các Use Cases nghiệp vụ lấy bảng tin, điều phối xếp hạng, trích xuất đặc trưng, gọi AI Client.
    *   *Domain Layer:* Thực thể nghiệp vụ cốt lõi và các cổng Repository Interfaces (áp dụng Dependency Inversion).
    *   *Infrastructure Layer:* Triển khai Adapter tương tác PostgreSQL (Spring Data JPA) và Redis (StringRedisTemplate).

## 4. [Frontend React Architecture](file:///home/phuquydam/Documents/Social-Pulse/docs/diagram_4_frontend_architecture.md)
*   **Mục tiêu:** Thể hiện kiến trúc tổng thể của giao diện web React (Vite + TypeScript + TailwindCSS + Shadcn/ui).
*   **Các thành phần chính:**
    *   *UI Hierarchy:* Luồng từ App Layout -> HomePage -> Feed List -> Post Card -> Post Media.
    *   *State & Connection Management:* Quản lý trạng thái đăng nhập, luồng đọc HTTP chunk thô để nhận sự kiện SSE (`RealTimeProvider` + `readSseStream`).
    *   *Hooks & Services:* Infinite Scroll qua Intersection Observer, API Client qua Axios Interceptor.
