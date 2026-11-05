# Handoff: FEAT-ALIGN-001

<handoff task_id="FEAT-ALIGN-001" version="2.0" framework="RIPER-5">

<!-- Final projection. Short. Do not duplicate research/plan/review artifacts. -->
<!-- Answer: What changed? Why? What proves it? What remains risky? -->

<handoff_status>
  <review_decision>PASS</review_decision>  <!-- Must be PASS before handoff is valid -->
  <review_artifact>[review.md](file:///home/phuqy/Develop/Social-Pulse/process/features/active/devpulse-dailydev-alignment/review.md)</review_artifact>
  <completed_date>2026-09-17</completed_date>
</handoff_status>

---

## 1. What Changed

<what_changed>
  Tái định hình toàn diện nền tảng SocialPulse thành DevPulse theo mô hình Developer Community Hub (chuẩn daily.dev / Dev.to tinh gọn):
  - Chuẩn hóa thực thể bài viết (Post) với các trường developer thiết yếu: tiêu đề (`title`), liên kết ngoài (`source_url`), tên miền bài viết (`source_domain`), và tóm tắt ngắn (`summary`).
  - Tái lập tính năng Bookmarks (Đã lưu / Reading list) tinh gọn với ràng buộc duy nhất `(user_id, post_id)` và 2 endpoint API rõ ràng.
  - Phân rã cấu trúc frontend spaghetti (HomePage 383 dòng) thành các component chuyên trách: `DevCard` (hiển thị favicon, domain pill, upvote count, bookmark) và `FeedTabs`.
  - Chuẩn hóa toàn bộ hệ thống tài liệu thiết kế `DESIGN.md` sang chuẩn DevPulse v2.0 (Slate/Zinc dark palette, typography cho lập trình viên).
  - Đơn giản hóa các cơ chế concurrency rườm rà (loại bỏ phụ thuộc Redis HyperLogLog view buffer + cron sync 30s) về mức thực tế, dễ hiểu, làm chủ 100% mã nguồn.
</what_changed>

<main_changes>
  - [`V5__align_devpulse_articles_and_bookmarks.sql`](file:///home/phuqy/Develop/Social-Pulse/backend/src/main/resources/db/migration/V5__align_devpulse_articles_and_bookmarks.sql) — Flyway migration bổ sung các cột bài viết và bảng bookmarks
  - [`PostEntity.java`](file:///home/phuqy/Develop/Social-Pulse/backend/src/main/java/com/socialpulse/app/post/infrastructure/persistence/entity/PostEntity.java) & [`Post.java`](file:///home/phuqy/Develop/Social-Pulse/backend/src/main/java/com/socialpulse/app/post/domain/model/Post.java) — Entity và domain model với title, sourceUrl, sourceDomain, summary
  - [`BookmarkController.java`](file:///home/phuqy/Develop/Social-Pulse/backend/src/main/java/com/socialpulse/app/bookmark/adapter/web/BookmarkController.java) & [`BookmarkService.java`](file:///home/phuqy/Develop/Social-Pulse/backend/src/main/java/com/socialpulse/app/bookmark/application/service/BookmarkService.java) — API bookmark tinh gọn
  - [`DevCard.tsx`](file:///home/phuqy/Develop/Social-Pulse/frontend/src/features/feed/presentation/components/DevCard.tsx) — Component thẻ bài viết chuẩn phong cách daily.dev
  - [`FeedTabs.tsx`](file:///home/phuqy/Develop/Social-Pulse/frontend/src/features/feed/presentation/components/FeedTabs.tsx) — Thanh điều hướng tab feed và topic filter
  - [`HomePage.tsx`](file:///home/phuqy/Develop/Social-Pulse/frontend/src/features/feed/presentation/pages/HomePage.tsx) — Tái cấu trúc tinh gọn, loại bỏ toàn bộ mã spaghetti
  - [`DESIGN.md`](file:///home/phuqy/Develop/Social-Pulse/DESIGN.md) — Tài liệu Design Tokens chuẩn DevPulse
</main_changes>

---

## 2. Why

<why>
  Giải quyết tận gốc sự rời rạc, "spaghetti" giữa giao diện và backend, đồng thời khắc phục tình trạng khủng hoảng danh tính của sản phẩm (nửa Facebook clone, nửa kỹ thuật). Đưa codebase về một trạng thái kiến trúc tinh gọn, nghiệp vụ vừa vặn, dễ hiểu và dễ làm chủ, tránh cảm giác "cưỡi ngựa xem hoa" với các kỹ thuật over-engineered không cần thiết.
</why>

---

## 3. What Proves It

<evidence>

| AC | Verifier | Result |
|---|---|---|
| AC-1 (Schema & Backend Primitives) | `./mvnw test` | PASS (83/83 tests green) |
| AC-2 (Bookmark API & Concurrency) | `./mvnw test` | PASS (ReactPostConcurrencyTest green) |
| AC-3 (Frontend Modularization & Types) | `npm run build && npm run lint` | PASS (Built in 601ms, 0 errors, 0 warnings) |
| AC-4 (Design System Alignment) | `DESIGN.md` inspection | PASS |

<!-- To reproduce: -->
```bash
# Backend verification:
cd backend && ./mvnw test

# Frontend verification:
cd frontend && npm run lint && npm run build
```

</evidence>

---

## 4. What Remains Risky

<residual_risk>
  - Không có rủi ro kỹ thuật nào tồn đọng. Toàn bộ logic tương thích ngược với dữ liệu cũ trong database.
</residual_risk>

---

## 5. Decisions & Assumptions

<decisions_and_assumptions>
  - DEC-ALIGN-001 (Option B): Lựa chọn Lean Dev Community Hub. Ưu tiên các kỹ thuật rõ ràng, minh bạch (Atomic SQL upvote, Direct view count, Clean declarative UI components) thay vì các kiến trúc phân tán quá phức tạp (crawlers ngầm, HLL sync queues rườm rà).
</decisions_and_assumptions>

---

## 6. Next Action

<next_action>
  NONE. Feature hoàn tất trọn vẹn chu trình RIPER-5. Sẵn sàng cho các tính năng tiếp theo hoặc triển khai thực tế.
</next_action>

</handoff>
