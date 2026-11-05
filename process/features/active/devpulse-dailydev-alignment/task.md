# Task: FEAT-ALIGN-001 DevPulse Core Re-alignment: Daily.dev Model & Codebase Restructuring

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>  <!-- BACKLOG | ACTIVE | BLOCKED | PAUSED | REVIEW | COMPLETED | CANCELLED -->
  <spec_level>S3</spec_level>  <!-- S0=sketch | S1=defined | S2=verified | S3=locked -->
  <priority>P1</priority>  <!-- P0=urgent | P1=high | P2=normal | P3=low -->
  <risk>LOW</risk>  <!-- LOW | MEDIUM | HIGH -->
  <estimated_story_points>5</estimated_story_points>  <!-- 1 SP ≈ 2-4 focused engineering hours -->
  <working_mode>PAIR</working_mode>  <!-- PAIR (default: halts at each gate for review) | DELEGATED (fast-track) -->
  <current_phase>REVIEW</current_phase>  <!-- RESEARCH | INNOVATE | PLAN | EXECUTE | REVIEW -->
  <owner>@phuqy</owner>
  <decision_owner>@phuqy</decision_owner>
  <created>2026-09-17</created>
  <last_updated>2026-09-17</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Conduct a comprehensive architectural, business, and UX research audit of the SocialPulse / DevPulse repository. Identify root causes of codebase fragmentation ("spaghetti" layering, disconnected frontend and backend, product identity crisis) and analyze alignment with a modern developer-centric platform model (such as daily.dev / Dev.to).
  </goal>

  <current_behavior>
    - Severe Product & Domain Mismatch:
      - Project repository is named `Social-Pulse`, backend README brands it as `DevPulse ⚡ (A High-Throughput Technical Community Platform)`, `DESIGN.md` contains an unintegrated copy of Meta hardware commerce tokens, and frontend UI is structured like a generic Facebook/Twitter status feed.
      - Post domain model is restricted to short status text (`content`) and image uploads (`imageUrl`), lacking essential developer content primitives: post titles, external article URLs, source publisher domains, reading times, markdown code blocks, and bookmarking.
      - A recent uncommitted refactoring pruned 7,700+ lines (chat, reports, blocks, bookmarks), leaving 160 modified/deleted files in the working tree and stripping out essential developer utility (e.g. saving/bookmarking technical articles).
    - Codebase & Frontend Organization:
      - `HomePage.tsx` is an overgrown 383-line monolith coupling auth checks, topic dropdowns, infinite scrolling, modal triggers, and feed rendering.
      - Backend implements a 6-layer hexagonal architecture around anemic domain entities, where real business logic directly touches JPA entities and JPQL queries.
  </current_behavior>

  <expected_behavior>
    - Produce an exhaustive `research.md` dissecting:
      1. Technical & architectural state (Backend Hexagonal vs Anemic Reality, Frontend Spaghetti State & Design Mismatch).
      2. Business & domain analysis (DevPulse vs Facebook-clone vs daily.dev comparative analysis).
      3. Frontend information architecture & UX paradigm shift (Daily.dev-style Dev Card, Reader view, Tag taxonomy, Bookmarks).
      4. Risk matrix and structural recommendations for the INNOVATE phase.
  </expected_behavior>

  <actor_authorization>
    System Architect, Lead Frontend Engineer, Lead Backend Engineer.
  </actor_authorization>

  <invariants>
    - READ-ONLY in RESEARCH phase: No source code modifications during research.
    - Concurrency strengths preserved: Atomic database math for upvoting, Redis HyperLogLog view buffering, and Singleflight feed caching must remain foundational backend primitives.
    - Evidence classification strictly tagged: Confirmed, Observed, or Hypothesized.
  </invariants>

  <out_of_scope>
    - Direct source code refactoring or schema migrations during the RESEARCH phase.
    - Final architectural decisions (deferred to Phase 2: INNOVATE / `decision.md`).
  </out_of_scope>

  <acceptance_criteria>
    - [ ] AC-1: Complete multi-dimensional audit documented in `research.md` (Codebase, Business, Frontend, daily.dev benchmark).
    - [ ] AC-2: Evidence properly categorized with file links and exact metrics.
    - [ ] AC-3: Exit criteria G0 satisfied for human review.
  </acceptance_criteria>

  <!-- Definition-of-Ready (DoR) Gate -->
  <definition_of_ready>
    - [x] Target outcome and acceptance criteria are verifiable without guessing.
    - [x] Invariants and <out_of_scope> boundaries are explicit.
    - [x] Open questions identified for Phase 2: INNOVATE.
  </definition_of_ready>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `README.md` — Project mission and architecture claims
    - `DESIGN.md` — Active design specification
    - `backend/src/main/java/com/socialpulse/app/` — Backend domain & hexagonal layers
    - `frontend/src/` — Frontend architecture, pages, and components
    - `backend/src/main/resources/db/migration/` — Database schema & seed data
  </target_files>

  <source_of_truth>
    <requirement>User prompt + RIPER-5 framework</requirement>
    <codebase_reality>Git tree status & source files</codebase_reality>
  </source_of_truth>
</context_boundaries>

</task_spec>
