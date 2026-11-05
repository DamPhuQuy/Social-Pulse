# Research: FEAT-ALIGN-001 DevPulse Core Re-alignment: Daily.dev Model & Codebase Restructuring

<research_context task_id="FEAT-ALIGN-001" version="3.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. No source modifications. No implementation decisions. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>READ-ONLY</mode>
  <research_owner>@phuqy</research_owner>
  <last_updated>2026-09-17</last_updated>
</research_status>

---

## 1. Executive Summary & Root Cause Synthesis

<executive_summary>
  An exhaustive audit of the Social-Pulse / DevPulse codebase reveals that the current repository suffers from a fundamental **three-way disconnect**:

  ```text
  ┌────────────────────────┐      ┌────────────────────────┐      ┌────────────────────────┐
  │   1. Product Claim     │      │  2. Codebase Reality   │      │   3. User Experience   │
  │  "DevPulse: High-      │  VS  │ • Backend: Status posts│  VS  │ • Twitter/FB clone UI  │
  │   Throughput Technical │      │   without Title / URL  │      │ • Unusable for tech    │
  │   Community Platform"  │      │ • Frontend: Spaghetti  │      │   articles / code      │
  │   (README.md)          │      │ • 160 uncommitted diffs│      │ • Meta VR DESIGN.md    │
  └────────────────────────┘      └────────────────────────┘      └────────────────────────┘
  ```

  ### Core Verdict
  1. **Product Identity Crisis**: The codebase originated as a university/bootcamp-style generic Facebook clone (`Social-Pulse` with 1:1 chat, user blocking, post reports, cover photos). Recently, backend documentation and isolated services were re-branded as `DevPulse` with distributed systems primitives (Redis HyperLogLog, Atomic voting math, Singleflight locks). However, the **business domain and the frontend were never updated to reflect a technical developer platform**.
  2. **Domain Model Inadequacy**: A developer platform (like [daily.dev](https://daily.dev), Dev.to, or Hacker News) is centered around **technical articles, tutorials, repositories, and engineering discussions**. The current `PostEntity` only supports `content` (plain text) and `imageUrl`. It has **no Title, no Article URL, no Source Domain, no Reading Time, no Markdown Code support**, and bookmarks were removed.
  3. **Frontend Spaghetti & Disconnection**: `HomePage.tsx` (383 lines) is a monolithic component tightly coupling theme logic, dropdown state, observer lifecycle, auth redirection, and modal toggles. The UI mimics a generic social media feed (author avatars, plain text statuses, heart/activity reactions, Instagram-style `#trending` hashtags).
  4. **The Daily.dev Opportunity**: The backend's high-concurrency features (HyperLogLog unique view tracking, singleflight feed caching, lost-update-free atomic upvoting) are **a perfect architectural match** for a daily.dev-style developer feed. Transitioning to this model resolves all contradictions.
</executive_summary>

---

## 2. Codebase Audit: Architecture vs. Reality

<codebase_audit>

### 2.1 Backend: Hexagonal Overhead vs. Anemic Reality
- **Layering Bloat**: Every domain feature contains up to 9 separate files across 5 sub-packages:
  - `adapter/web/PostController.java`
  - `application/usecase/CreatePostUseCase.java`
  - `application/service/CreatePostService.java`
  - `application/dto/request/PostCreationRequest.java`
  - `domain/model/Post.java`
  - `domain/repository/PostRepository.java`
  - `adapter/persistence/PostRepositoryAdapter.java`
  - `infrastructure/persistence/entity/PostEntity.java`
  - `infrastructure/persistence/repository/JpaPostRepository.java`
- **Anemic Domain Layer** (`[Observed]`): Despite the Hexagonal structure, domain models (`Post.java`, `Comment.java`) are passive data containers. Real business logic completely bypasses them:
  - `ReactPostService.java` executes direct JPQL updates on `PostEntity` in `JpaPostRepository`.
  - `GetFeedService.java` queries JPA repositories directly and maps straight to DTOs via response enrichers.
- **Git Working Tree Inconsistency** (`[Confirmed]`): 160 files are currently modified or deleted in the working tree (`-7,715 lines`). Non-core modules (`chat`, `report`) were abruptly removed, but bookmarks (`BookmarkEntity`, `bookmarkService.ts`, `BookmarksPage.tsx`) were also deleted despite bookmarks being a critical pillar for any developer platform.

### 2.2 Frontend: State Coupling & Architectural Spaghetti
- **God Component Monolith** (`[Confirmed]` in `frontend/src/features/feed/presentation/pages/HomePage.tsx`):
  - Line count: 383 lines.
  - Holds 12 distinct React state hooks (`currentUser`, `trendingTags`, `isDark`, `showCreateModal`, `isDropdownOpen`, `dropdownSearch`, `reactingPostIds`, `editingPost`, `sharingPost`, plus feed hooks).
  - Handles infinite scrolling via inline `IntersectionObserver`.
  - Inlines the entire topic selection dropdown popover.
- **Design System Disconnect** (`[Confirmed]`):
  - `DESIGN.md` contains 684 lines describing Meta hardware commerce (Meta Quest VR, Ray-Ban Meta AI glasses, Optimistic VF font, ecommerce purchase buttons).
  - The actual frontend code completely ignores `DESIGN.md`, using ad-hoc inline Tailwind utility classes with arbitrary hex codes (`bg-[#1e1e1e]`, `dark:border-[#2a2a2a]`, `text-[#e4e6eb]`, `bg-[#0064e0]`).
- **Data Model Duplication** (`[Confirmed]`):
  - `FeedItem` in `postService.ts` defines 25 fields.
  - `UserPost` in `userService.ts` defines 16 fields for the exact same entity with different key names.
  - `OriginalPostData` defines yet another structure.
- **Language & UX Inconsistency** (`[Observed]`):
  - Sidebar menus are in Vietnamese: `Trang chủ`, `Khám phá`, `Thông báo`, `Hồ sơ`, `Cài đặt`, `Giám sát AI`, `Phân quyền RBAC`.
  - Top header and cards are in English/Vietnamese hybrid.
  - Hashtags are styled as `#XuHuong` rather than developer topics (`#react`, `#rust`, `#docker`).

</codebase_audit>

---

## 3. Business & Product Analysis: DevPulse vs. Daily.dev

<business_analysis>

### 3.1 Comparison Matrix

| Dimension | Current SocialPulse / DevPulse | Target: Daily.dev Model | Gap / Problem |
|---|---|---|---|
| **Core Value Prop** | Generic social timeline (status updates + images) | **Homepage for Developers**: curated dev news, tutorials, GitHub repos, engineering blogs | Zero technical content primitives currently exist. |
| **Primary Unit of Content** | Plain text status (`content` + optional `imageUrl`) | **Developer Card**: Title, External URL, Source Publisher, Reading Time, TL;DR, Tech Tags | No `title`, no `url`, no `source_domain`, no `reading_time`. |
| **Content Types** | Original post or Reshare | 1. **Curated/Shared Article** (external link + preview)<br>2. **Tech Discussion** (native question/guide with Markdown + Code) | Missing discrimination between external articles and internal discussions. |
| **Engagement Primitives** | Upvote (styled as generic "Activity" icon), Comment | Atomic Upvote, Developer Discussions, **Save/Bookmark to Reading List**, Share | Bookmarks were deleted; upvote icon is confusing. |
| **Taxonomy & Discovery** | Generic hashtags (`#XuHuong`) + Flat topics | **Tech Stacks & Squads** (`#webdev`, `#ai`, `#golang`, `#system-design`, `#kubernetes`) | Discovery searches Instagram-like hashtags instead of tech taxonomy. |
| **Reading Experience** | Expandable comment thread underneath status | **Clean Reader View**: Headline, Source Link (external redirect), Markdown rendering with syntax highlighting, nested tech comments | No code formatting, no markdown renderer, no link preview. |

### 3.2 Why the Daily.dev Model Validates the Backend Concurrency Architecture

The backend's most impressive engineering feats were built to solve distributed systems bottlenecks:
1. **Redis HyperLogLog Unique View Deduplication (`PFADD`) + Write-Behind Flush**:
   - *In a generic social network*: View counts on random status updates rarely matter.
   - *In a Daily.dev aggregator*: Viral technical articles (e.g. "New React 19 Compiler Deep Dive") receive massive traffic surges. In-memory HLL deduplication and batched write-behind flushing directly solve this exact viral traffic spike.
2. **Redis Singleflight Distributed Lock on Feed Generation**:
   - *In a generic social network*: Personalized social graph queries are user-specific.
   - *In a Daily.dev aggregator*: Shared feeds (e.g., "Trending Today", "Top in #Rust") expire and trigger severe Thundering Herd / Cache Stampedes. Singleflight distributed locking prevents database death.
3. **Atomic Row-Level Upvoting (`CASE WHEN ...`)**:
   - *In a generic social network*: Likes are vanity metrics.
   - *In a Daily.dev aggregator*: Upvoting directly drives the Hot/Trending algorithm (`hot_score`), experiencing viral contention on frontpage posts. Atomic math eliminates lost updates completely.

</business_analysis>

---

## 4. Frontend Redesign: Daily.dev Paradigm

<frontend_redesign_paradigm>

### 4.1 Information Architecture & Layout (Modern Dev Theme)

```text
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ [DevPulse Logo]     [ Search developer articles, tags, squads... (Ctrl+K) ]   [New Post] [User] │
├──────────────┬─────────────────────────────────────────────────────────┬───────────────┤
│ FEEDS        │ [ My Feed ]  [ 🔥 Popular ]  [ ⚡ Latest ]  [ 💬 Discussions ] │ TRENDING TAGS │
│ • My Feed    ├─────────────────────────────────────────────────────────┤ • #ai-agents  │
│ • Popular    │ ┌─────────────────────────────────────────────────────┐ │ • #rust       │
│ • Latest     │ │ github.com/facebook/react • 4 min read              │ │ • #systemdesign│
│ • Discuss    │ │ React 19 Source Code Deep Dive: How Actions Work   │ │ • #kubernetes │
│              │ │ A detailed walkthrough of the new action queue...   │ ├───────────────┤
│ LIBRARY      │ │ [#react] [#frontend] [#javascript]                  │ │ TOP DISCUSSIONS│
│ • Bookmarks  │ │ ▲ 248  •  💬 32 comments  •  🔖 Save                │ │ "Postgres vs  │
│ • History    │ └─────────────────────────────────────────────────────┘ │  MongoDB in   │
│              │ ┌─────────────────────────────────────────────────────┐ │  2026?"       │
│ SQUADS / TAGS│ │ martinfowler.com • 8 min read                       │ └───────────────┘
│ • #golang    │ │ Modular Monoliths vs Microservices in Practice      │                 │
│ • #ai        │ │ ▲ 512  •  💬 89 comments  •  🔖 Save                │                 │
│ • #systemdes │ └─────────────────────────────────────────────────────┘                 │
└──────────────┴─────────────────────────────────────────────────────────┴───────────────┘
```

### 4.2 Core Frontend Component Hierarchy
To eliminate spaghetti, the frontend should be restructured into single-responsibility, composable modules:
1. **`DevCard`** (`features/feed/presentation/components/DevCard.tsx`):
   - **Source Header**: Favicon/Source icon + publisher domain (e.g. `blog.cloudflare.com`) + reading time + publication date.
   - **Title**: Large, bold headline (navigates to Reader view or opens source directly).
   - **Summary / Excerpt**: 2-line preview or AI TL;DR.
   - **Tags Bar**: Tech badges (`#typescript`, `#docker`).
   - **Dev Action Bar**:
     - Upvote counter with distinct developer styling (`▲ Upvote` with count).
     - Comments counter (`💬 24`).
     - **Save / Bookmark button** (`🔖`).
     - External link trigger (`↗ Open source`).
2. **`FeedTabs`** (`features/feed/presentation/components/FeedTabs.tsx`):
   - Seamless switcher between "My Feed", "Popular", "Recent", and "Discussions".
3. **`AppSidebar` Restructuring**:
   - Organized into clear sections: **Feeds**, **Library** (Saved/Bookmarks, Reading History), and **Followed Tech Stacks**.
4. **`PostDetail / ReaderView`**:
   - Clean reading mode supporting **Markdown rendering** (`react-markdown` + `prismjs` / `shiki` syntax highlighting for code blocks).
   - Author attribution + link to original source.
   - Threaded technical comments.

</frontend_redesign_paradigm>

---

## 5. Evidence Classification

<evidence_classification>

### Confirmed Evidence (`[Confirmed]`)
- `README.md` asserts DevPulse is a "High-Throughput Technical Community Platform" ([README.md#L1-L25](file:///home/phuqy/Develop/Social-Pulse/README.md#L1-L25)).
- `DESIGN.md` describes a Meta hardware commerce system completely detached from the actual app ([DESIGN.md#L1-L50](file:///home/phuqy/Develop/Social-Pulse/DESIGN.md#L1-L50)).
- `PostEntity.java` contains only `content`, `imageUrl`, `imagePublicId`, `topicSlugs`, and counter fields. It completely lacks `title`, `url`, `sourceDomain`, `summary`, and `readingTimeMinutes` ([PostEntity.java#L48-L88](file:///home/phuqy/Develop/Social-Pulse/backend/src/main/java/com/socialpulse/app/post/infrastructure/persistence/entity/PostEntity.java#L48-L88)).
- `HomePage.tsx` is an overgrown 383-line component with 12 state variables and inline observers ([HomePage.tsx#L29-L90](file:///home/phuqy/Develop/Social-Pulse/frontend/src/features/feed/presentation/pages/HomePage.tsx#L29-L90)).
- Git working tree contains 160 uncommitted modified/deleted files, including the removal of bookmarks ([git diff](file:///home/phuqy/Develop/Social-Pulse/)).

### Observed Evidence (`[Observed]`)
- In-memory domain entities (`Post.java`, `Comment.java`) are bypassed in favor of direct JPQL mutations on JPA entities.
- Upvote interaction uses an ambiguous "Activity" pulse icon instead of standard developer platform upvotes.
- The UI language is an uneven mix of Vietnamese administrative labels and English technical terms.

### Hypothesized Evidence (`[Hypothesized]`)
- Restoring and upgrading the Bookmarks domain into a "Developer Reading List" will increase user retention and make the platform feel like a daily developer tool.
- Transitioning the Post model to support both `ARTICLE` (curated link) and `DISCUSSION` (markdown thread) will harmonize the backend concurrency features with the frontend presentation.

</evidence_classification>

---

## 6. Gap & Risk Analysis

<risk_analysis>

| Risk ID | Risk Description | Severity | Mitigation Strategy in Next Phases |
|---|---|---|---|
| **R1** | **Database Schema Migration**: Adding `title`, `url`, `source_domain`, `summary`, `reading_time` requires a Flyway migration `V5__align_devpulse_schema.sql`. | Medium | Write backward-compatible migration: default existing posts to `title = SUBSTRING(content, 1, 60)`, make URL optional for discussion posts. |
| **R2** | **Dirty Git State**: 160 modified files with uncommitted removals (chat, reports). | High | Cleanly commit the pruning of chat/reports, but selectively preserve or re-architect the bookmarking model for developer reading lists. |
| **R3** | **Frontend Component Regression**: Refactoring `HomePage.tsx` and `PostCard.tsx` could break existing voting/commenting flows. | Medium | Decompose into modular components with explicit props and unit/integration tests before swapping. |

</risk_analysis>

---

## 7. Research Exit Criteria (Gate G0)

<exit_criteria>
  - [x] Codebase structure and technical debt thoroughly audited and documented.
  - [x] Root causes of architectural spaghetti and domain mismatch identified.
  - [x] Business and product requirements benchmarked against daily.dev model.
  - [x] Frontend redesign paradigm and component decomposition mapped out.
  - [x] All evidence classified as Confirmed, Observed, or Hypothesized.
  - [x] Ready for Gate G0 review and transition to Phase 2: INNOVATE.
</exit_criteria>

</research_context>
