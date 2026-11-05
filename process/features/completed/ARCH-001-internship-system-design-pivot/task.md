# Task: ARCH-001 Internship System Design & Portfolio Architecture Pivot

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>
  <spec_level>S1</spec_level>
  <priority>P0</priority>
  <risk>HIGH</risk>
  <estimated_story_points>8</estimated_story_points>
  <working_mode>PAIR</working_mode>
  <current_phase>COMPLETED</current_phase>
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-11</created>
  <last_updated>2026-09-11</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Pivot Social Pulse from an unranked, generic 23-table CRUD social media clone into a high-impact, production-grade technical showcase engineered specifically to stand out in backend / software engineering internship interviews.
    This is achieved by:
    1. Pruning low-value, bloated CRUD modules (1-on-1 chat, blocks, moderation reports, bookmarks, redundant RBAC joins).
    2. Refocusing domain purpose around high-velocity technical discussions / developer feed (HackerNews / DevPulse archetype).
    3. Solving and benchmarking core distributed systems and concurrency problems: race condition handling in voting/views, Redis atomic counter & rate limiting, hybrid feed fan-out (push/pull), cache stampede mitigation, and Java 21 Virtual Threads vs Platform Threads.
    4. Providing reproducible load-test benchmarks (k6) and deployment readiness (Docker, Actuator/Prometheus metrics, CI/CD).
  </goal>

  <current_behavior>
    - 471 Java files, 23 database tables, and 113 frontend files covering basic CRUD for posts, comments, reactions, bookmarks, blocks, follows, 1-on-1 chat, reports, and search history.
    - Concurrency vulnerabilities: ReactPostService and ViewPostService execute unprotected in-memory read-modify-write patterns (post.setUpvoteCount(...) -> save()), leading to lost updates and lock contention under concurrent load.
    - Feed generation uses a pure pull model (fan-out on read) executing 3 separate SQL queries with joins on every feed fetch, resulting in severe latency under scale.
    - No stress/load testing benchmarks, no distributed locks, no idempotency guarantees, no live cloud deployment setup or observability metrics.
  </current_behavior>

  <expected_behavior>
    - Clean, purpose-built domain focused on high-throughput technical discussions and ranking.
    - Zero race conditions: Atomic database increments, Redis Lua scripts, or optimistic locking (@Version) with idempotency keys.
    - Hybrid / Caching Feed Architecture: Redis Sorted Sets (ZSET) for precomputed timelines, cache-aside with stampede protection.
    - Java 21 Concurrency Tuning: Virtual threads enabled, comparative throughput benchmarks documented.
    - Reproducible k6 benchmark suite demonstrating RPS, p95 latency, and zero data corruption before vs. after optimizations.
    - Clear, resume-ready architectural narrative and live deployment setup.
  </expected_behavior>

  <actor_authorization>
    System architect and software engineering candidate optimizing project codebase for interview evaluation.
  </actor_authorization>

  <invariants>
    - Core Clean Architecture layer separation (Domain -> Application -> Adapter -> Infrastructure) must be preserved.
    - Existing authentication (JWT + refresh token) must remain secure and operational.
    - All business-critical operations must be verifiable via automated tests and load scripts.
    - Destructive or uncoordinated file deletions outside the approved plan are strictly prohibited.
  </invariants>

  <out_of_scope>
    - Rebuilding an entirely new frontend from scratch (keep frontend lean and aligned with pruned API).
    - Implementing complex distributed microservices (monolith with Redis is optimal and most defensible for internship scope).
    - Advanced machine learning models for ranking (keep ranking deterministic and explainable).
  </out_of_scope>

  <acceptance_criteria>
    - [ ] AC-1: Comprehensive code audit documenting existing bloat, database tables, and CRUD vs. high-concurrency hotspots completed in research.md.
    - [ ] AC-2: Pruning roadmap defined to remove or archive non-essential CRUD modules (chat, block, report, bookmark) and reduce surface area.
    - [ ] AC-3: Technical architecture blueprint established for Concurrency (race conditions, atomic updates, idempotency) and Feed Scaling (Redis ZSET, cache-aside).
    - [ ] AC-4: Java 21 concurrency (Virtual Threads) and load testing strategy (k6 benchmark scripts) formulated.
    - [ ] AC-5: Research phase Exit Criteria (Gate G0) verified and validated for human review.
  </acceptance_criteria>

  <definition_of_ready>
    - [x] Target outcome and acceptance criteria are verifiable without guessing.
    - [x] Invariants and out_of_scope boundaries are explicit.
    - [x] Open questions identified and prepared for INNOVATE phase (decision.md).
  </definition_of_ready>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `backend/src/main/java/com/socialpulse/app/post/application/service/ReactPostService.java`
    - `backend/src/main/java/com/socialpulse/app/post/application/service/ViewPostService.java`
    - `backend/src/main/java/com/socialpulse/app/feed/application/service/ranking/FeedRankingService.java`
    - `backend/src/main/java/com/socialpulse/app/feed/application/service/candidate/CandidateSelectionService.java`
    - `backend/src/main/resources/application.yaml`
    - `backend/src/main/resources/db/migration/`
    - `docker-compose.yaml`
  </target_files>

  <context_groups>
    - planning
    - tests
    - protocols
  </context_groups>

  <source_of_truth>
    <requirement>User request: Pivot Social Pulse to high-concurrency system design project for internship application</requirement>
    <architecture>DESIGN.md, README.md, backend/pom.xml</architecture>
    <existing_behavior>backend/src/main/resources/db/migration/V1__init.sql</existing_behavior>
    <tests>backend/src/test/</tests>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1: Code Audit & Concurrency Hotspots | Audit tables, class hierarchy, and race-condition analysis in research.md | Static code analysis & RIPER-5 G0 review |
| AC-2: Pruning Roadmap | List of target deprecations and slimmed domain boundary | Specification & dependency tree check |
| AC-3: Architecture Blueprint | Concrete designs for Redis Lua atomic updates, idempotency, and Feed Caching | Design & sequence diagrams in decision.md |
| AC-4: Benchmark & Virtual Threads | k6 test script designs & thread configuration parameters | JMeter/k6 plan and config verifier |
| AC-5: Gate G0 Sign-off | All exit criteria checked in research.md | Gate G0 checklist |

</verification_strategy>

---

## 4. Decisions

<decisions>
  <approved_decisions>
    <!-- Filled after Innovate phase -->
  </approved_decisions>

  <open_decisions>
    | ID | Question | Why it matters | Owner | Blocking? |
    |---|---|---|---|---|
    | DEC-01 | Domain repositioning: Developer Tech Pulse vs. High-Velocity Event Voting | Dictates product persona and API ergonomics | @engineer | Yes |
    | DEC-02 | Pruning depth: Complete deletion vs. Feature flag deprecation | Determines refactoring risk vs. repository cleanliness | @engineer | Yes |
    | DEC-03 | Concurrency strategy: Database-level atomic queries vs. Redis write-behind buffer | Impacts complexity, persistence guarantees, and benchmark throughput | @engineer | Yes |
  </open_decisions>
</decisions>

---

## 5. RIPER-5 Execution Plan (Pillar 4: Loop)

<execution_plan>
  <phase name="Research" order="1">
    - [x] Ingest task spec, domain invariants, and out-of-scope boundaries.
    - [x] Read corresponding tests and port interfaces.
    - [x] Establish execution flow, boundaries, and source-of-truth conflicts.
    - [x] Produce/update research.md artifact.
    <gate id="G0" label="Research Complete">
      - [x] Current behavior understood and documented.
      - [x] Execution flow traced.
      - [x] No unresolved research blocker.
    </gate>
  </phase>

  <phase name="Innovate" order="2">
    - [ ] Generate 2–3 alternative approaches with trade-off matrix.
    - [ ] Produce decision.md artifact.
    <gate id="G1" label="Gate 1 — Decision Approved">
      - [x] Options reviewed and trade-offs analyzed.
      - [x] Selected option recorded in decision.md.
      - [x] No blocking business/schema/security decision remains open.
      <approved_by>@engineer</approved_by>
      <approved_date>2026-09-11</approved_date>
    </gate>
  </phase>

  <phase name="Plan" order="3">
    - [ ] Decompose into vertical slices with verifiers and rollback points.
    - [ ] Populate plan.md with scope contract and verification matrix.
    <gate id="G2" label="Gate 2 — Plan Approved">
      - [x] Every slice has a verifier.
      - [x] Allowed/forbidden file scope is defined.
      - [x] Rollback point defined per slice.
      - [x] Stop conditions defined.
      - [x] Plan approved.
      <approved_by>@engineer</approved_by>
      <approved_date>2026-09-11</approved_date>
    </gate>
  </phase>

  <phase name="Execute" order="4">
    - [ ] Implement each slice atomically.
    - [ ] Run verifier after each slice.
    - [ ] Inspect diff after each slice.
    - [ ] Update state.md after each slice.
    - [ ] Record quantitative metric deltas in results.tsv.
  </phase>

  <phase name="Review" order="5">
    - [ ] Review full diff, behavior, architecture, data, security, regression.
    - [ ] Produce review.md with findings and verification matrix.
    <gate id="G3" label="Gate 3 — Review Passed">
      - [ ] All AC verified with evidence.
      - [ ] Residual risk accepted.
      - [ ] Review decision: PASS.
      - [ ] Ready for handoff.
      <approved_by></approved_by>
      <approved_date></approved_date>
    </gate>
  </phase>
</execution_plan>

---

## 6. Guardrails & Escalation (Pillar 3 & 4: Harness)

<guardrails>
  <stop_conditions>
    - Missing business or domain pivot decision from user.
    - Public API / DB schema destruction outside approved plan.
    - Destructive git operations or unconstrained mass deletions.
    - Retry budget exhausted on a recurring failure.
  </stop_conditions>

  <retry_budget max_attempts="3">
    Maximum 3 consecutive attempts per distinct failure symptom before halting.
  </retry_budget>
</guardrails>

</task_spec>
