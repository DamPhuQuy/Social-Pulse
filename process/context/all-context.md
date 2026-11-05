# Root Context Router

<context_router version="1.0">

<description>
  Central durable knowledge index routing to specialized domain context groups across the system.
</description>

## Context Directory Index

<!--
  CONTEXT ROUTING GUIDELINES:
  - Central index for all project domain knowledge, architecture, and invariants.
  - When adding a new context file or directory (e.g. context/auth/, context/database/),
    ALWAYS register a new <group> entry below.
  - Specify clear `scope` so agents know exactly when to load this context group without
    unnecessary full-directory scanning.
-->
<context_groups>
  <group id="planning">
    <title>Planning Standards & Calibration</title>
    <path>[`planning/all-planning.md`](planning/all-planning.md)</path>
    <scope>INVEST criteria, vertical slicing rules, Story Point capacity calibration</scope>
  </group>

  <group id="seeds">
    <title>Seeds & Archetypes Guide</title>
    <path>[`../_seeds/_GUIDE.md`](../_seeds/_GUIDE.md)</path>
    <scope>Scaffolding seeds for tasks, research, decisions, plans, state, reviews, handoffs, pause, cancellation, benchmarks, and programs. Full artifact chain and instantiation commands.</scope>
  </group>

  <group id="tests">
    <title>Testing Standards & Harness Strategy</title>
    <path>[`tests/all-tests.md`](tests/all-tests.md)</path>
    <scope>Test pyramid, unit/integration isolation policies, mock conventions, and test fixtures</scope>
  </group>

  <group id="protocols">
    <title>Development Protocols Index</title>
    <path>[`../development-protocols/all-development-protocols.md`](../development-protocols/all-development-protocols.md)</path>
    <scope>Engineering harness, subagent orchestration, and code standards</scope>
  </group>

  <group id="concurrency">
    <title>Concurrency, System Design & Benchmarking Guide</title>
    <path>[`concurrency-and-system-design-guide.md`](concurrency-and-system-design-guide.md)</path>
    <scope>Race conditions, lost updates, atomic SQL, Redis Lua scripts, HyperLogLog, Java 21 Virtual Threads, and k6 load benchmarking.</scope>
  </group>

  <group id="devops_benchmark_cicd">
    <title>Benchmark, DevOps & CI/CD Engineering Playbook</title>
    <path>[`../../docs/BENCHMARK_DEVOPS_CICD_GUIDE.md`](../../docs/BENCHMARK_DEVOPS_CICD_GUIDE.md)</path>
    <scope>WHAT, WHY, HOW for k6 load testing, Linux VPS containerized deployment (Multi-stage Docker, Caddy HTTPS), GitHub Actions CI/CD pipeline, and Google XYZ Resume conversion.</scope>
  </group>
</context_groups>

</context_router>
