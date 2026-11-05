# Plan: PLAN-ALIGN-001 DevPulse Pragmatic Lean Re-alignment (Daily.dev Model)

<execution_plan task_id="FEAT-ALIGN-001" plan_id="PLAN-ALIGN-001" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-17</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>[FEAT-ALIGN-001 task.md](file:///home/phuqy/Develop/Social-Pulse/process/features/active/devpulse-dailydev-alignment/task.md)</task_spec>
  <research>[research.md](file:///home/phuqy/Develop/Social-Pulse/process/features/active/devpulse-dailydev-alignment/research.md)</research>
  <decision>[decision.md — DEC-ALIGN-001, Option B](file:///home/phuqy/Develop/Social-Pulse/process/features/active/devpulse-dailydev-alignment/decision.md)</decision>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `backend/src/main/resources/db/migration/V5__align_devpulse_articles_and_bookmarks.sql` — Flyway schema migration
    - `backend/src/main/java/com/socialpulse/app/post/` — Post entity, DTOs, service, controller
    - `backend/src/main/java/com/socialpulse/app/feed/` — Feed response DTOs and mapper
    - `backend/src/main/java/com/socialpulse/app/bookmark/` — Lean bookmark domain, controller, service, repository
    - `backend/src/main/java/com/socialpulse/app/common/schedule/SyncSchedule.java` — Simplify/prune over-engineered view sync
    - `frontend/src/features/feed/` — DevCard, FeedTabs, HomePage decomposition
    - `frontend/src/features/bookmarks/` — Lean bookmark API and Saved Posts view
    - `frontend/src/shared/components/AppSidebar.tsx` — Sidebar navigation restructure
    - `DESIGN.md` — Real DevPulse design tokens (replacing Meta VR specs)
  </allowed_files>
  <forbidden_files>
    - `backend/src/main/java/com/socialpulse/app/security/` — Core JWT auth logic
    - `.github/workflows/` — CI pipelines
    - `docker-compose*.yaml` — Container infrastructure
  </forbidden_files>
  <allowed_commands>
    - `./mvnw test-compile`
    - `./mvnw test`
    - `npm run lint`
    - `npm run build`
    - `git status`
    - `git diff`
  </allowed_commands>
  <restricted_operations>
    - Không viết background crawler/scraper phức tạp.
    - Không can thiệp hoặc làm hỏng logic Atomic Update SQL của Post upvote.
    - Giữ DB migration backward-compatible với dữ liệu cũ.
  </restricted_operations>
  <required_approvals>
    - Phê duyệt Plan trước khi chạy Execute (Gate G2).
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S1 | Schema Migration & Post Entity Alignment | Database + Backend Entity | `V5__...sql`, `PostEntity.java` | AC-1 | `./mvnw test-compile` | Low | PAIR | git checkout entity & drop migration |
| S2 | Backend DTOs, Service & Lean Bookmarks | Backend Application Layer | `post/`, `feed/`, `bookmark/` | AC-1, AC-2 | `./mvnw test` | Low | PAIR | git checkout backend |
| S3 | Frontend API Client & Types Alignment | Frontend Core API | `feed/api`, `bookmarks/api` | AC-3 | `npm run lint` | Low | PAIR | git checkout api files |
| S4 | Frontend Decomposition: DevCard & Clean Feed | Frontend Presentation | `DevCard.tsx`, `FeedTabs.tsx`, `HomePage.tsx` | AC-3 | `npm run build` | Medium | PAIR | git checkout presentation |
| S5 | Design Tokens & End-to-End Validation | Design Docs + Integration | `DESIGN.md`, `README.md` | AC-4 | Full stack verification | Low | PAIR | git diff inspect |

</slice_summary>

---

## 4. Slice Details

<slices>

  <!-- SLICE 1 -->
  <slice id="S1">
    <objective>Thêm các trường thiết yếu của bài viết developer (title, source_url, source_domain, summary) và bảng bookmarks vào database.</objective>
    <change>
      - Tạo Flyway migration `V5__align_devpulse_articles_and_bookmarks.sql`.
      - Cập nhật `PostEntity.java` với: `title`, `sourceUrl`, `sourceDomain`, `summary`.
    </change>
    <allowed_files>
      - `backend/src/main/resources/db/migration/V5__align_devpulse_articles_and_bookmarks.sql`
      - `backend/src/main/java/com/socialpulse/app/post/infrastructure/persistence/entity/PostEntity.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: Entity và schema tương thích, compile thành công.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./mvnw test-compile
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESS</expected_evidence>
    <rollback_point>git checkout backend/src/main/java/com/socialpulse/app/post/infrastructure/persistence/entity/PostEntity.java && rm backend/src/main/resources/db/migration/V5__align_devpulse_articles_and_bookmarks.sql</rollback_point>
    <stop_conditions>
      - Lỗi compile JPA entity.
    </stop_conditions>
  </slice>

  <!-- SLICE 2 -->
  <slice id="S2">
    <objective>Cập nhật DTOs, Service của Post/Feed và triển khai tính năng Bookmark tinh gọn (2 endpoints: toggle và get).</objective>
    <change>
      - Thêm `title`, `sourceUrl`, `sourceDomain`, `summary` vào `PostCreationRequest`, `PostResponse`, `FeedItemResponse`.
      - Trong `CreatePostService`, tự động bóc tách `sourceDomain` từ `sourceUrl` nếu có (dùng `URI.create(url).getHost()`).
      - Tạo `BookmarkController` và `BookmarkService` đơn giản.
      - Đơn giản hóa cơ chế đếm view: dùng update trực tiếp, loại bỏ phụ thuộc rối rắm vào `SyncSchedule`.
    </change>
    <allowed_files>
      - `backend/src/main/java/com/socialpulse/app/post/`
      - `backend/src/main/java/com/socialpulse/app/feed/`
      - `backend/src/main/java/com/socialpulse/app/bookmark/`
      - `backend/src/main/java/com/socialpulse/app/common/schedule/SyncSchedule.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1, AC-2: Post API trả về đầy đủ title và source_domain; Bookmark API hoạt động trơn tru.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./mvnw test-compile
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESS with 0 errors</expected_evidence>
    <rollback_point>git checkout backend/src/main/java/com/socialpulse/app/</rollback_point>
    <stop_conditions>
      - Gặp lỗi tương thích mapper hoặc test logic.
    </stop_conditions>
  </slice>

  <!-- SLICE 3 -->
  <slice id="S3">
    <objective>Cập nhật kiểu dữ liệu TypeScript và Axios API client ở Frontend.</objective>
    <change>
      - Cập nhật interface `FeedItem`, `PostCreationRequest` trong `postService.ts`.
      - Tạo `bookmarkService.ts` với hàm `toggleBookmark(postId)` và `getMyBookmarks()`.
      - Thêm trạng thái `isBookmarked` vào `FeedItem`.
    </change>
    <allowed_files>
      - `frontend/src/features/feed/infrastructure/api/postService.ts`
      - `frontend/src/features/bookmarks/infrastructure/api/bookmarkService.ts`
      - `frontend/src/shared/hooks/usePostActions.ts`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-3: Typescript compile sạch, không có lỗi kiểu.
    </acceptance_criteria>
    <verifier>
      ```bash
      npm run lint
      ```
    </verifier>
    <expected_evidence>0 errors</expected_evidence>
    <rollback_point>git checkout frontend/src/features/feed/infrastructure/api/postService.ts</rollback_point>
    <stop_conditions>
      - Xung đột type định nghĩa giữa các feature.
    </stop_conditions>
  </slice>

  <!-- SLICE 4 -->
  <slice id="S4">
    <objective>Module hóa HomePage.tsx và xây dựng DevCard chuẩn developer (daily.dev style).</objective>
    <change>
      - Tách `HomePage.tsx` (383 dòng) thành các component chuyên trách:
        - `DevCard.tsx`: Thẻ bài viết tinh gọn (Domain nguồn + Favicon, Tiêu đề to rõ, Tóm tắt, Tech tags, Nút Upvote `▲`, Số thảo luận, Nút Bookmark `🔖`).
        - `FeedTabs.tsx`: Tab chuyển "Dành cho bạn", "Mới nhất", "Thảo luận".
      - Cải tiến modal đăng bài `CreatePostModal.tsx` để hỗ trợ nhập Title và Link bài viết.
      - Cập nhật `AppSidebar.tsx` rõ ràng (Feeds, Đã lưu 🔖, Tech tags).
    </change>
    <allowed_files>
      - `frontend/src/features/feed/presentation/components/DevCard.tsx`
      - `frontend/src/features/feed/presentation/components/FeedTabs.tsx`
      - `frontend/src/features/feed/presentation/components/CreatePostModal.tsx`
      - `frontend/src/features/feed/presentation/pages/HomePage.tsx`
      - `frontend/src/shared/components/AppSidebar.tsx`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-3: Giao diện gọn gàng, hết spaghetti, build Vite thành công.
    </acceptance_criteria>
    <verifier>
      ```bash
      npm run build
      ```
    </verifier>
    <expected_evidence>vite build successfully</expected_evidence>
    <rollback_point>git checkout frontend/src/features/feed/</rollback_point>
    <stop_conditions>
      - Lỗi build React / JSX.
    </stop_conditions>
  </slice>

  <!-- SLICE 5 -->
  <slice id="S5">
    <objective>Cập nhật DESIGN.md sang chuẩn DevPulse Design Tokens và kiểm thử tích hợp toàn diện.</objective>
    <change>
      - Thay thế `DESIGN.md` (hiện là token Meta Quest VR) bằng tài liệu Design Tokens thực tế của DevPulse (Slate/Zinc dark-mode, tech badges, dev typography).
      - Đồng bộ `README.md` mô tả đúng sản phẩm thực tế: một Developer Community & Content Hub tinh gọn, hiệu quả.
    </change>
    <allowed_files>
      - `DESIGN.md`
      - `README.md`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-4: Toàn bộ tài liệu và code khớp 100% với nhau.
    </acceptance_criteria>
    <verifier>
      ```bash
      git status
      ```
    </verifier>
    <expected_evidence>Clean tree, consistent naming</expected_evidence>
    <rollback_point>git checkout DESIGN.md README.md</rollback_point>
    <stop_conditions>
      - Không có.
    </stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - Thêm các trường thiết yếu cho Post: `title`, `source_url`, `source_domain`, `summary`.
    - Tạo bảng và logic `bookmarks` tinh gọn (user_id, post_id).
    - Tách component `HomePage.tsx` thành `DevCard.tsx`, `FeedTabs.tsx`.
    - Cập nhật `DESIGN.md` và `README.md` cho chuẩn xác.
  </allowed>
  <forbidden>
    - Không sửa các logic bảo mật JWT cốt lõi trong `security/`.
    - Không viết crawler / scraper chạy ngầm.
    - Không làm hỏng logic Atomic Update upvote trong `PostRepository`.
  </forbidden>
</scope_contract>

---

## 6. Verification Matrix

<verification_matrix>

| AC / Risk | Test / Command | Expected Evidence | Strictness | Actual Result |
|---|---|---|---|---|
| AC-1 (Schema & Entity) | `./mvnw test-compile` | BUILD SUCCESS | hard-mandatory | Pending S1 |
| AC-2 (Backend DTO & Bookmark) | `./mvnw test-compile` | BUILD SUCCESS | hard-mandatory | Pending S2 |
| AC-3 (Frontend Lint & Build) | `npm run build` | Built in x ms, 0 errors | hard-mandatory | Pending S3-S4 |
| AC-4 (Docs & Design Consistency) | `git status` | Clean status | soft-mandatory | Pending S5 |

</verification_matrix>

---

## Gate 2 — Plan Approved

<gate id="G2">
  - [x] Every slice has a defined verifier.
  - [x] Scope contract (allowed / forbidden) approved.
  - [x] Enforcement strictness assigned per AC/verification item.
  - [x] Stop conditions defined per slice.
  - [x] Rollback point defined per slice.
  - [x] Allowed commands listed.
  - [x] Plan approved by engineer.
  <approved_by>@phuqy</approved_by>
  <approved_date>2026-09-17</approved_date>
</gate>

</execution_plan>
