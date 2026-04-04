# Hướng dẫn AGENTS cho social-pulse
# Hướng dẫn này nhằm 
## Tổng quan dự án 
- Monorepo hiện có một service có thể chạy được: backend Spring Boot trong `backend/` và tài liệu DB trong `docs/db/`.
- Mô hình runtime là app + Postgres qua Docker Compose (`docker-compose.yaml`): backend phụ thuộc vào service `db` ở trạng thái healthy.
- Điểm vào (entrypoint) của Java là `backend/src/main/java/com/socialpulse/app/Application.java`.
- Codebase hiện theo hướng data-model-first: chủ yếu là JPA entities + Flyway migrations, phần application layer còn tối giản.

## Source of Truth và phạm vi
- Xem Flyway SQL trong `backend/src/main/resources/db/migration/` là schema mang tính chuẩn (authoritative).
- Hibernate được cấu hình `ddl-auto: validate` trong `backend/src/main/resources/application.yaml`; lệch giữa entity/schema sẽ làm ứng dụng không khởi động được.
- `docs/db/database.dbml` có các bảng/trường ở mức kế hoạch chưa được implement đầy đủ (ví dụ: `notifications`, `reports`, nested comments); không mặc định DBML là trạng thái đang chạy thực tế.
- Giữ naming tương thích với quy ước SQL hiện có (nhiều bảng/cột được đặt tên non-standard có chủ đích, ví dụ `comment`, `reaction`, `userid`, `postid`).
## Kiến trúc và ranh giới domain
- Các package được tổ chức theo domain dưới `com.socialpulse.app`: `user`, `auth`, `post`, `comment`, `reaction`, `social`, `common`.
- `common/entity` (`City`, `Category`, `Topic`, `Hashtag`) đóng vai trò dữ liệu tham chiếu/lookup dùng chung.
- Luồng lõi là nội dung do người dùng tạo: `User` -> `Post` -> `Comment`, kèm các bảng reaction tách riêng (`reaction`, `reactioncmt`) và social graph (`follows`).
- Quan hệ many-to-many dùng join entity tường minh với composite ID (`PostHashtag`, `UserTopic`) thay vì hidden join table.

## Coding patterns cần tuân theo
- Entities dùng nhất quán Lombok (`@Getter/@Setter/@Builder/@NoArgsConstructor/@AllArgsConstructor`) và Jakarta Validation annotations.
- Mặc định quan hệ đa phần là lazy (`FetchType.LAZY`); giữ nguyên trừ khi chủ đích thay đổi hành vi API/query.
- Trường thời gian dùng Hibernate annotations (`@CreationTimestamp`, `@UpdateTimestamp`) thay vì DB trigger.
- Cách dùng enum đang pha trộn: `Follow.status` là `@Enumerated(EnumType.STRING)`, trong khi `OtpCode.type` hiện là `String` thuần.
- Tên trong SQL và entity liên kết chặt chẽ; nếu đổi tên field/column, cần cập nhật đồng bộ entity và migration tương ứng.

## Quy trình làm việc cho developer
- Cách chạy local ưu tiên là Docker Compose từ thư mục gốc repo:
```bash
# từ D:\Projects\social-pulse
docker compose up --build
```
- Quy trình Maven chỉ cho backend (từ `backend/`):
```bash
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```
- Compose và app config yêu cầu env vars `POSTGRES_DB`, `POSTGRES_USERNAME`, `POSTGRES_PASSWORD` (xem `docker-compose.yaml` và `backend/src/main/resources/application.yaml`).
- Lưu ý: datasource host mặc định là `db`; khi chạy backend ngoài Compose, cần override datasource URL/host tương ứng.

## Quy tắc thay đổi cho agents
- Thêm thay đổi schema bằng file Flyway phiên bản mới có đánh số (`V16__...sql`, `V17__...sql`, ...); không chỉnh sửa lịch sử migration đã có.
- Với tính năng persistence mới, triển khai đồng thời SQL migration và JPA mapping tương ứng trong cùng một thay đổi.
- Khai báo indexes/constraints tường minh trong SQL, theo style hiện tại (mỗi migration tạo bảng thường định nghĩa index ngay bên dưới phần table definition).
- Bộ test hiện còn tối thiểu (`backend/src/test/java/com/socialpulse/app/AppApplicationTests.java`); nếu thêm hành vi vượt ngoài entities, hãy bổ sung test tập trung cho hành vi đó.
