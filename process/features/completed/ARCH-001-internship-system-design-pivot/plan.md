# Plan: PLAN-001 DevPulse Architecture & Concurrency Implementation

<execution_plan task_id="ARCH-001" plan_id="PLAN-001" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-11</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>[`task.md`](task.md)</task_spec>
  <research>[`research.md`](research.md)</research>
  <decision>[`decision.md`](decision.md) — DEC-001 (Option B: Hybrid Redis Lua + Atomic SQL + k6 Benchmarks)</decision>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `backend/src/main/java/com/socialpulse/app/chat/**` (Prune)
    - `backend/src/main/java/com/socialpulse/app/report/**` (Prune)
    - `backend/src/main/java/com/socialpulse/app/block/**` (Prune)
    - `backend/src/main/java/com/socialpulse/app/bookmark/**` (Prune)
    - `backend/src/main/java/com/socialpulse/app/post/**`
    - `backend/src/main/java/com/socialpulse/app/feed/**`
    - `backend/src/main/resources/db/migration/V5__prune_unused_tables.sql`
    - `backend/src/main/resources/application.yaml`
    - `backend/src/test/**`
    - `frontend/src/**` (Pruning navigation & routes only)
    - `benchmarks/k6/**` (New load testing scripts)
    - `docker-compose.yaml`
    - `README.md`
  </allowed_files>
  <forbidden_files>
    - `backend/src/main/java/com/socialpulse/app/auth/**` (Auth core must remain intact)
    - `backend/src/main/java/com/socialpulse/app/security/jwt/**` (JWT verification must remain intact)
    - `backend/src/main/resources/db/migration/V1__init.sql` (Existing historical migrations must never be altered)
  </forbidden_files>
  <allowed_commands>
    - `cd backend && ./mvnw clean compile`
    - `cd backend && ./mvnw test`
    - `cd backend && ./mvnw test -Dtest=...`
    - `cd frontend && npm run build`
    - `docker compose ...`
  </allowed_commands>
  <restricted_operations>
    - No destructive modifications to historical Flyway migrations (V1-V4). All schema changes must happen in new migrations (V5+).
    - No changes outside declared vertical slices.
  </restricted_operations>
  <required_approvals>
    - Approval of Gate G2 before commencing code modifications in Slice 1.
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S1 | Prune bloat modules (chat, report, block, bookmark) & DB tables | Backend & DB migration | ~50 files deleted/updated | AC-2 | `./mvnw clean compile test` | MED | PAIR | git checkout main / restore branch |
| S2 | Atomic Concurrency & Race Condition Elimination for Voting | `post` module | 6 files | AC-3 | `./mvnw test -Dtest=ReactPostConcurrencyTest` | LOW | PAIR | git checkout slice checkpoint |
| S3 | View Count Buffering (Write-Behind & HyperLogLog) | `post` + Redis buffer | 4 files | AC-3 | `./mvnw test -Dtest=ViewPostServiceTest` | LOW | PAIR | git checkout slice checkpoint |
| S4 | High-Performance Feed & Caching (Redis Timelines) | `feed` module | 5 files | AC-3 | `./mvnw test -Dtest=*Feed*` | MED | PAIR | git checkout slice checkpoint |
| S5 | Java 21 Virtual Threads & k6 Load Benchmark Suite | Config + k6 scripts | 5 files | AC-4 | k6 execution run | LOW | PAIR | git checkout slice checkpoint |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S1">
    <objective>Excise bloated non-essential modules (chat, report, block, bookmark) from backend and frontend, and create Flyway V5 migration dropping unused tables.</objective>
    <change>
      - Remove `backend/src/main/java/com/socialpulse/app/{chat,report,block,bookmark}`.
      - Remove references to `BlockRepository` in `CandidateSelectionService.java`.
      - Create `backend/src/main/resources/db/migration/V5__prune_unused_tables.sql` dropping `conversations`, `messages`, `reports`, `user_blocks`, `bookmarks`.
      - Clean up corresponding frontend routes and navigation bars.
    </change>
    <allowed_files>
      - `backend/src/main/java/com/socialpulse/app/chat/**`
      - `backend/src/main/java/com/socialpulse/app/report/**`
      - `backend/src/main/java/com/socialpulse/app/block/**`
      - `backend/src/main/java/com/socialpulse/app/bookmark/**`
      - `backend/src/main/java/com/socialpulse/app/feed/application/service/candidate/CandidateSelectionService.java`
      - `backend/src/main/resources/db/migration/V5__prune_unused_tables.sql`
      - `frontend/src/**`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-2: Pruning roadmap executed; backend compiles cleanly and passes all remaining unit tests.
    </acceptance_criteria>
    <verifier>
      ```bash
      cd backend && ./mvnw clean test-compile
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESS with 0 compilation errors.</expected_evidence>
    <rollback_point>git restore . && git clean -fd</rollback_point>
    <stop_conditions>
      - Any compilation breakage in core modules (`auth`, `post`, `feed`, `user`) that cannot be resolved cleanly.
    </stop_conditions>
  </slice>

  <slice id="S2">
    <objective>Eliminate lost-update race conditions and double-vote crashes in post reactions using Atomic SQL and Redis Lua scripting.</objective>
    <change>
      - Implement atomic increment/decrement repository method: `UPDATE posts SET upvote_count = upvote_count + :delta WHERE id = :id`.
      - Implement idempotent upsert for `post_reactions` with `ON CONFLICT` handling.
      - Add multi-threaded concurrency integration test (`ReactPostConcurrencyTest.java`) running 100 parallel threads on the same post.
    </change>
    <allowed_files>
      - `backend/src/main/java/com/socialpulse/app/post/**`
      - `backend/src/test/java/com/socialpulse/app/post/**`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-3: Zero lost updates under 100 concurrent threads; reaction counts strictly equal DB reaction log rows.
    </acceptance_criteria>
    <verifier>
      ```bash
      cd backend && ./mvnw test -Dtest=ReactPostConcurrencyTest
      ```
    </verifier>
    <expected_evidence>ReactPostConcurrencyTest: 100 concurrent operations executed, final count strictly matches expected value, 0 lost updates.</expected_evidence>
    <rollback_point>git checkout HEAD -- backend/src/main/java/com/socialpulse/app/post/</rollback_point>
    <stop_conditions>
      - Test failure indicating lost update > 0.
    </stop_conditions>
  </slice>

  <slice id="S3">
    <objective>Implement View Count write-behind buffering and unique view counting via Redis HyperLogLog.</objective>
    <change>
      - Decouple `ViewPostService.java` from direct DB row updates.
      - Store unique view attempts in Redis HyperLogLog (`PFADD post:{id}:views {userId}`).
      - Buffer view deltas in Redis and flush to PostgreSQL in a scheduled batch task.
    </change>
    <allowed_files>
      - `backend/src/main/java/com/socialpulse/app/post/**`
      - `backend/src/main/resources/application.yaml`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-3: Viewing a post does not execute an immediate synchronous database row lock.
    </acceptance_criteria>
    <verifier>
      ```bash
      cd backend && ./mvnw test -Dtest=ViewPostServiceTest
      ```
    </verifier>
    <expected_evidence>Tests verify view increments buffered in Redis and batch flushed.</expected_evidence>
    <rollback_point>git checkout HEAD -- backend/src/main/java/com/socialpulse/app/post/</rollback_point>
    <stop_conditions>
      - Redis connection or serialization error.
    </stop_conditions>
  </slice>

  <slice id="S4">
    <objective>Optimize Feed generation with Redis Sorted Set (ZSET) timeline caching and cache stampede protection.</objective>
    <change>
      - Cache precomputed topic and hot feeds in Redis Sorted Sets (`ZSET`).
      - Implement singleflight lock during cache refresh to prevent cache stampedes.
    </change>
    <allowed_files>
      - `backend/src/main/java/com/socialpulse/app/feed/**`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-3: Cached feed fetches return in < 15ms without querying PostgreSQL candidate tables.
    </acceptance_criteria>
    <verifier>
      ```bash
      cd backend && ./mvnw test -Dtest=*Feed*
      ```
    </verifier>
    <expected_evidence>All feed tests pass with cache hits verified.</expected_evidence>
    <rollback_point>git checkout HEAD -- backend/src/main/java/com/socialpulse/app/feed/</rollback_point>
    <stop_conditions>
      - Invalidation inconsistency between post creation and feed cache.
    </stop_conditions>
  </slice>

  <slice id="S5">
    <objective>Enable Java 21 Virtual Threads and build containerized k6 load benchmark suite.</objective>
    <change>
      - Set `spring.threads.virtual.enabled: true` in `application.yaml`.
      - Create `benchmarks/k6/` with `high-contention-voting.js` and `feed-throughput.js`.
      - Add benchmark documentation and resume-ready metrics to `README.md`.
    </change>
    <allowed_files>
      - `backend/src/main/resources/application.yaml`
      - `benchmarks/k6/**`
      - `docker-compose.yaml`
      - `README.md`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-4: k6 scripts run successfully against the server, reporting throughput, p95/p99 latency, and 0% errors.
    </acceptance_criteria>
    <verifier>
      ```bash
      docker compose up -d db redis backend
      # Run k6 scenario
      ```
    </verifier>
    <expected_evidence>k6 output summary showing RPS, p95 < 50ms, 0% failure rate.</expected_evidence>
    <rollback_point>git checkout HEAD -- benchmarks/ README.md</rollback_point>
    <stop_conditions>
      - Memory leaks or thread pinning under virtual threads.
    </stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - Deleting pruned modules: `chat`, `report`, `block`, `bookmark`.
    - Adding `V5__prune_unused_tables.sql` Flyway migration.
    - Updating `post`, `feed`, and configuration to support atomic queries, Redis buffers, and Virtual Threads.
    - Adding `benchmarks/k6` directory with load testing scripts.
    - Updating `README.md` with DevPulse identity and architecture diagrams.
  </allowed>
  <forbidden>
    - Modifying existing Flyway migrations V1 to V4.
    - Changing user authentication security contracts (JWT, password hash).
    - Introducing Kafka or unnecessary heavy infrastructure brokers.
  </forbidden>
</scope_contract>

---

## 6. Verification Matrix

<verification_matrix>

| AC / Risk | Test / Command | Expected Evidence | Strictness | Actual Result |
|---|---|---|---|---|
| AC-1: Audit & Hotspots | Inspection of `research.md` | Complete inventory and race condition traces | hard-mandatory | PASS |
| AC-2: Bloat Pruning | `cd backend && ./mvnw clean test-compile` | BUILD SUCCESS, 0 errors | hard-mandatory | Pending S1 |
| AC-3: Concurrency Safety | `cd backend && ./mvnw test -Dtest=ReactPostConcurrencyTest` | 0% lost updates under 100 threads | hard-mandatory | Pending S2 |
| AC-3: View Count Buffering | `cd backend && ./mvnw test -Dtest=ViewPostServiceTest` | Batch flush verified, 0 row lock bottlenecks | soft-mandatory | Pending S3 |
| AC-3: Feed Latency | `cd backend && ./mvnw test -Dtest=*Feed*` | Sub-15ms cached responses | soft-mandatory | Pending S4 |
| AC-4: k6 Benchmarks | k6 execution report | RPS > 1,500, p95 < 50ms, 0% error rate | advisory | Pending S5 |

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
  <approved_by>@engineer</approved_by>
  <approved_date>2026-09-11</approved_date>
</gate>

</execution_plan>
