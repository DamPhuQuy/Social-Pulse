# Decision: DEC-ALIGN-001 DevPulse Pragmatic Lean Re-alignment (Daily.dev Model)

<technical_decision task_id="FEAT-ALIGN-001" dec_id="DEC-ALIGN-001" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. Present options. PAIR: engineer decides. -->
<decision_status>
  <phase>INNOVATE</phase>
  <mode>READ-ONLY</mode>
  <decision_owner>@phuqy</decision_owner>
  <last_updated>2026-09-17</last_updated>
</decision_status>

---

## 1. Context

<context>
  <task>[FEAT-ALIGN-001 task.md](file:///home/phuqy/Develop/Social-Pulse/process/features/active/devpulse-dailydev-alignment/task.md)</task>
  <research_artifact>[research.md](file:///home/phuqy/Develop/Social-Pulse/process/features/active/devpulse-dailydev-alignment/research.md)</research_artifact>
  <constraints>
    - Nghiệp vụ và tính năng phải "vừa đủ mức độ, không quá phức tạp nhưng vẫn đủ để giữ mức độ hợp lý".
    - Không ôm đồm các hệ thống background crawler, RSS scraper phức tạp hay phân quyền Squads đa tầng.
    - Giải quyết triệt để sự rời rạc ("spaghetti") giữa UI, Frontend và Backend.
    - Bảo toàn và tận dụng 100% các ưu thế kỹ thuật phân tán hiện có: Atomic math voting, Redis HyperLogLog view buffering, Redis Singleflight feed lock.
  </constraints>
</context>

---

## 2. Decision Required

<decision_question>
  Lựa chọn phạm vi và kiến trúc tái cấu trúc cho DevPulse để đạt được sự đồng bộ hoàn chỉnh giữa nghiệp vụ, frontend và backend theo mô hình developer community / content feed gọn gàng, hợp lý.
</decision_question>

---

## 3. Options

<options>

  <!-- OPTION A -->
  <option id="A">
    <approach>
      **Option A: Surface-Level Facelift (Vá giao diện, giữ nguyên Backend)**
      - Giữ nguyên toàn bộ schema DB hiện tại (không thêm cột `title`, `source_url`).
      - Frontend tự cắt 60 ký tự đầu của `content` làm tiêu đề hiển thị, tự parse URL nếu có trong text.
      - Không có tính năng Bookmark thực sự (hoặc lưu tạm trong LocalStorage trình duyệt).
    </approach>
    <advantages>
      - Rất ít code backend, không cần chạy Flyway migration mới.
    </advantages>
    <disadvantages>
      - Nửa vời: Bản chất dữ liệu trong DB vẫn chỉ là status Facebook ngắn.
      - Trải nghiệm lọc bài, tìm kiếm và phân loại nguồn tin bị chắp vá, thiếu bền vững.
      - Không giải quyết được căn nguyên "spaghetti và không liên quan gì nhau".
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>HIGH</compatibility>
    <concurrency_transaction_risk>LOW</concurrency_transaction_risk>
    <testability>MEDIUM</testability>
    <maintainability>LOW</maintainability>
  </option>

  <!-- OPTION B -->
  <option id="B">
    <approach>
      **Option B: Lean Dev Community Hub (Mô hình Daily.dev thu gọn — RECOMMENDED)**
      - **Nghiệp vụ cốt lõi vừa vặn**:
        - Bài đăng chia làm 2 dạng thực tế:
          1. **Article / Curated Link**: Người dùng chia sẻ link bài viết kỹ thuật hay (Github, Medium, Dev.to, blog cá nhân) -> có `title`, `source_url`, `source_domain` tự động trích xuất, và `summary` ngắn.
          2. **Tech Discussion**: Bài hỏi đáp, chia sẻ kiến trúc nội bộ với `title` và `content` hỗ trợ Markdown.
        - Phục hồi tính năng **Bookmarks (Reading List)** gọn nhẹ: 1 bảng quan hệ `user_id, post_id, created_at` với API lưu/bỏ lưu và xem danh sách đã lưu.
      - **Backend tinh gọn**:
        - 1 Flyway migration duy nhất (`V5__add_article_fields_and_bookmarks.sql`) bổ sung các cột nullable vào `posts`, tái lập bảng `bookmarks`.
        - Tận dụng trọn vẹn: Atomic upvoting, Redis HyperLogLog view buffer, Singleflight feed lock.
      - **Frontend chuyên nghiệp & Module hóa**:
        - Phá vỡ God-Component `HomePage.tsx` thành các component độc lập: `DevCard`, `FeedTabs`, `TechTagsFilter`.
        - Thẻ bài viết `DevCard` chuẩn dev: Tiêu đề rõ ràng, domain nguồn + favicon, tech tags (`#react`, `#rust`), upvote `▲`, comment count, nút bookmark `🔖`.
        - Thay thế `DESIGN.md` Meta VR bằng theme Dev chuẩn: Slate/Zinc dark-mode, font chữ sạch, không màu mè.
    </approach>
    <advantages>
      - Đạt đúng yêu cầu: Nghiệp vụ vừa đủ, hợp lý, không quá phức tạp nhưng cực kỳ mạch lạc và thực tế.
      - Frontend, Backend và UI thống nhất 100% thành một sản phẩm Developer Content & Community chân chính.
      - Làm nổi bật tối đa giá trị của các thuật toán concurrency backend.
      - Dọn dẹp sạch sẽ 160 file git diff dang dở.
    </advantages>
    <disadvantages>
      - Cần 1 migration nhỏ trong DB và cập nhật DTO của Post.
    </disadvantages>
    <complexity>MEDIUM</complexity>
    <compatibility>HIGH (Backward-compatible với các post cũ)</compatibility>
    <concurrency_transaction_risk>LOW (Không đụng chạm logic lock/atomic)</concurrency_transaction_risk>
    <testability>HIGH</testability>
    <maintainability>HIGH</maintainability>
  </option>

  <!-- OPTION C -->
  <option id="C">
    <approach>
      **Option C: Full-blown Daily.dev Ecosystem (Hệ thống phức tạp toàn diện)**
      - Xây dựng background worker tự động crawl RSS từ 50+ nguồn báo tech (TechCrunch, HackerNews, Dev.to, InfoQ).
      - Triển khai tính năng Squads với phân quyền Admin/Member đa cấp.
      - Xây dựng Chrome Extension đồng bộ bookmark và New Tab.
      - Hệ thống Dev Karma, Leaderboard, Reading Streaks.
    </approach>
    <advantages>
      - Tính năng phong phú như một nền tảng thương mại lớn.
    </advantages>
    <disadvantages>
      - Quá phức tạp (vượt xa mong muốn của người dùng: "không quá phức tạp").
      - Chi phí bảo trì hạ tầng cao (crawler dễ bị block, crawler failures, tốn tài nguyên server).
      - Rủi ro bug và phân mảnh dự án tăng vọt.
    </disadvantages>
    <complexity>HIGH</complexity>
    <compatibility>LOW</compatibility>
    <concurrency_transaction_risk>HIGH</concurrency_transaction_risk>
    <testability>LOW</testability>
    <maintainability>LOW</maintainability>
  </option>

</options>

---

## 4. Trade-off Matrix

<tradeoff_matrix>

| Tiêu chí | Option A (Surface Facelift) | Option B (Lean Daily.dev Hub) ⭐ | Option C (Full Ecosystem) |
|---|:---:|:---:|:---:|
| **Mức độ vừa vặn nghiệp vụ** | 2/5 (Quá thiếu) | **5/5 (Vừa đủ, hợp lý)** | 2/5 (Quá phức tạp) |
| **Sự mạch lạc UI - FE - BE** | 2/5 (Vẫn spaghetti) | **5/5 (Thống nhất hoàn toàn)** | 4/5 (Rộng nhưng cồng kềnh) |
| **Tận dụng Concurrency BE** | 3/5 | **5/5 (Phát huy 100% thế mạnh)**| 4/5 |
| **Độ phức tạp triển khai** | 1/5 (Rất thấp) | **2.5/5 (Vừa phải, kiểm soát tốt)**| 5/5 (Rất nặng nề) |
| **Độ ổn định & Dễ bảo trì** | 2/5 | **5/5 (Kiến trúc sạch sẽ)** | 2/5 (Dễ gãy vỡ do crawler) |

</tradeoff_matrix>

---

## 5. Recommendation

<recommendation>
  **Lựa chọn khuyến nghị: Option B (Lean Dev Community Hub)**.
  
  Lý do:
  1. Đáp ứng chính xác mong muốn của bạn: *"kiểu nghiệp vụ, features đủ mức độ, không quá phức tạp nhưng vẫn đủ để giữ mức độ hợp lí"*.
  2. Bổ sung đúng 4 thuộc tính còn thiếu của bài viết dev (`title`, `source_url`, `source_domain`, `summary`) và 1 bảng quan hệ `bookmarks`.
  3. Không cần hệ thống crawl phức tạp: người dùng tự chia sẻ link hoặc đăng thảo luận.
  4. Triệt tiêu hoàn toàn sự spaghetti ở frontend: module hóa `HomePage.tsx` và tạo `DevCard` sắc sảo, hiện đại chuẩn developer.
</recommendation>

---

## 6. Implementation Decision

<!-- PAIR mode: Đang chờ Engineer phê duyệt và ký Gate 1 -->
<engineer_decision>
  <selected_option>Option B: Lean Dev Community Hub (Pragmatic & Understandable Engineering)</selected_option>
  <rationale>
    Kỹ sư phê duyệt hướng tiếp cận thực dụng, dễ hiểu, làm chủ 100% codebase. Loại bỏ các phần over-engineered gây mơ hồ (như HyperLogLog sync rườm rà), đưa các kỹ thuật (SQL atomic math, Redis cache) về mức độ trực quan, dễ bảo trì, dễ tự tin giải thích. Bổ sung các trường cốt lõi của bài viết developer (title, link, tags, summary) và khôi phục bookmarks gọn nhẹ. Tái cấu trúc frontend thành các component module hóa theo chuẩn DevCard.
  </rationale>
  <rejected_alternatives>
    - Option A: Bị từ chối vì chắp vá, không giải quyết được bản chất domain và sự rời rạc giữa các tầng.
    - Option C: Bị từ chối vì quá phức tạp, cồng kềnh và tốn tài nguyên bảo trì không cần thiết.
  </rejected_alternatives>
</engineer_decision>

---

## 7. Constraints Created by This Decision

<constraints_created>
  - Migration DB (`V5`) phải đảm bảo backward-compatible (các post cũ được gán title tự động từ 60 ký tự đầu của content).
  - Không thêm các background batch jobs crawl dữ liệu từ ngoài.
  - Loại bỏ hoặc tối giản hóa cơ chế đếm view phức tạp (HyperLogLog + SyncSchedule), chuyển sang direct atomic increment hoặc standard Redis counter.
  - Thiết kế UI tập trung vào phong cách dark-mode tinh gọn, card rõ ràng, font đọc tốt (không dùng lại token Meta VR trong DESIGN.md).
</constraints_created>

---

## 8. Evidence Still Required

<evidence_required>
  - Kiểm chứng compile backend (`./mvnw test-compile`) và frontend build (`npm run build`) sau khi cập nhật model.
</evidence_required>

---

## Gate 1 — Decision Approved

<gate id="G1">
  - [x] All options presented and trade-offs analyzed.
  - [x] Selected option recorded above.
  - [x] No blocking business / schema / security decision remains open.
  <approved_by>@phuqy</approved_by>
  <approved_date>2026-09-17</approved_date>
</gate>

</technical_decision>
