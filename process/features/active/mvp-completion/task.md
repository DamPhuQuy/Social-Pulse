# Task: FEAT-MVP-001 MVP Hardening, Verification & Final Stabilization

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>ACTIVE</status>  <!-- BACKLOG | ACTIVE | BLOCKED | PAUSED | REVIEW | COMPLETED | CANCELLED -->
  <spec_level>S1</spec_level>  <!-- S0=sketch | S1=defined | S2=verified | S3=locked -->
  <priority>P1</priority>  <!-- P0=urgent | P1=high | P2=normal | P3=low -->
  <risk>MEDIUM</risk>  <!-- LOW | MEDIUM | HIGH -->
  <estimated_story_points>3</estimated_story_points>  <!-- 1 SP ≈ 2-4 focused engineering hours -->
  <working_mode>PAIR</working_mode>  <!-- PAIR (default: halts at each gate for review) | DELEGATED (fast-track: continuous autonomous run) | MANUAL | DIAGNOSE-ONLY -->
  <current_phase>RESEARCH</current_phase>  <!-- RESEARCH | INNOVATE | PLAN | EXECUTE | REVIEW -->
  <owner>@phuqy</owner>
  <decision_owner>@phuqy</decision_owner>
  <created>2026-09-15</created>
  <last_updated>2026-09-15</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Audit, harden, and verify the DevPulse MVP technical platform: validate end-to-end functionality across backend (Java 21/Spring Boot) and frontend (React 19), fix outstanding TypeScript lint errors, consolidate Git working tree state, and ensure full concurrency benchmark compliance under k6.
  </goal>

  <current_behavior>
    The project has completed a major architectural re-alignment:
    - Pruned non-essential CRUD bloat (chat, bookmarks, reporting) to establish a clean technical community platform.
    - Implemented high-concurrency primitives: Atomic voting math (lost-update free), Redis HyperLogLog view count write-behind buffer, and Redis singleflight distributed feed cache lock.
    - Backend passes 83 unit tests and 3 concurrency tests.
    - Frontend build compiles successfully via Vite, but has 3 ESLint warnings/errors (explicit 'any') and working tree has uncommitted refactor diffs.
  </current_behavior>

  <expected_behavior>
    - Zero frontend lint errors (`npm run lint` passes cleanly).
    - Full end-to-end verification of user journeys (Auth, Post Creation, Atomic Voting, HLL View Recording, Comments, Feed Caching).
    - Docker compose dev/prod environments verified for turnkey deployment.
    - Automated k6 load benchmarks validated against SLA thresholds.
  </expected_behavior>

  <actor_authorization>
    Standard Authenticated Developer/User & System Administrator.
  </actor_authorization>

  <invariants>
    - Zero Lost Updates: Concurrent votes on posts must guarantee 100% arithmetic balance accuracy.
    - Sub-30ms p95 Latency: Feed retrieval and in-memory view buffering must meet SLA targets.
    - Non-blocking I/O: Java 21 Project Loom Virtual Threads remain active for all request handling.
    - Clean Architecture: Hexagonal / Clean architecture boundaries strictly preserved.
  </invariants>

  <out_of_scope>
    - Re-introducing removed non-core features (1:1 chat, bookmark collection, user report workflows).
    - Rewriting the Feed Ranking algorithm or changing database schemas.
  </out_of_scope>

  <acceptance_criteria>
    - [ ] AC-1: Frontend static analysis clean: `npm run lint` passes with 0 errors and 0 warnings.
    - [ ] AC-2: Backend test suite fully verified: `./mvnw test` passes 83/83 unit tests and `./mvnw test -Dtest=ReactPostConcurrencyTest` passes 3/3 concurrency tests.
    - [ ] AC-3: Full-stack containerization check: `docker compose config` is valid and containers build successfully.
    - [ ] AC-4: Benchmark suite execution: k6 scripts for voting contention, feed throughput, and view buffering execute against target SLAs.
  </acceptance_criteria>

  <!-- Definition-of-Ready (DoR) Gate — Frontload the thinking before dispatch -->
  <definition_of_ready>
    - [x] Target outcome and acceptance criteria are verifiable without guessing.
    - [x] Invariants and <out_of_scope> boundaries are explicit.
    - [x] Open questions resolved or scheduled in decision.md (No speculative coding).
  </definition_of_ready>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `frontend/src/app/App.tsx` — Fix explicit any lint error
    - `frontend/src/features/profiles/presentation/pages/ProfilePage.tsx` — Fix explicit any lint errors
    - `backend/src/main/java/com/socialpulse/app/post/` — Atomic math and HLL view services
    - `backend/src/main/java/com/socialpulse/app/feed/` — Singleflight feed cache
    - `benchmarks/k6/` — Load testing scenarios
  </target_files>

  <context_groups>
    - planning
    - tests
    - concurrency
  </context_groups>

  <source_of_truth>
    <requirement>README.md and DESIGN.md</requirement>
    <architecture>README.md (Architecture Diagram & Concurrency Showcase)</architecture>
    <existing_behavior>backend/src/test/java/com/socialpulse/app/post/ReactPostConcurrencyTest.java</existing_behavior>
    <tests>backend/src/test/ and frontend/src/</tests>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1 (Lint) | 0 lint errors | `cd frontend && npm run lint` |
| AC-2 (Tests) | All 83+ unit and concurrency tests pass | `cd backend && ./mvnw test` |
| AC-3 (Build) | Frontend bundle & Backend JAR built | `cd frontend && npm run build && cd ../backend && ./mvnw test-compile` |
| AC-4 (Benchmarks) | k6 test exit code 0 | `docker run --rm -i --network=host -v $(pwd)/benchmarks/k6:/scripts grafana/k6 run ...` |

</verification_strategy>

---

## 4. Execution Plan (Pillar 4: Loop)

<execution_plan>
  <current_phase>RESEARCH</current_phase>

  <!-- Gates G0–G3 mark phase boundaries -->
  <gates>
    <gate id="G0" phase="RESEARCH">
      - [x] Current behavior and execution flows documented in research.md
      - [x] Existing tests and gaps identified
      - [x] Evidence classified (Confirmed / Observed / Hypothesized)
      - [ ] Research findings approved by engineer
    </gate>
    <gate id="G1" phase="INNOVATE">
      - [ ] Options with trade-off matrix in decision.md
      - [ ] Recommendation selected
      - [ ] Engineer sign-off on decision.md
    </gate>
    <gate id="G2" phase="PLAN">
      - [ ] Vertical slices defined with explicit verifiers
      - [ ] Scope contract: allowed/forbidden files explicit
      - [ ] Engineer sign-off on plan.md
    </gate>
    <gate id="G3" phase="REVIEW">
      - [ ] Code review across 5 dimensions (behavior, arch, data, security, regression)
      - [ ] All verifiers passing
      - [ ] Clean git diff
      - [ ] Final sign-off before completion
    </gate>
  </gates>
</execution_plan>

</task_spec>
