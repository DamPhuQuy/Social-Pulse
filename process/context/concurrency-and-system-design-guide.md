# Concurrency & System Design Engineering Guide (WHAT, WHY, HOW)

<guide version="1.0" topic="concurrency-and-benchmarking">

<overview>
This durable guide documents core distributed systems, database concurrency, in-memory caching, and benchmarking concepts implemented during the DevPulse architectural transformation. 
Use this document as both a technical reference for this codebase and a study guide for software engineering / backend internship interviews.
</overview>

---

## 1. Race Conditions & The Lost Update Anomaly

### WHAT is it?
A **race condition** occurs when multiple concurrently running execution threads access and modify shared state, and the final state depends on the non-deterministic scheduling order of the threads.
The **Lost Update Anomaly** is a classic race condition where two transactions read the same initial data, compute new values independently in memory, and write back—resulting in one transaction silently overwriting (and wiping out) the other transaction's update.

### WHY does it happen in Web Applications?
In a typical Spring Boot web application, each incoming HTTP request is assigned its own worker thread.
When two users click "Upvote" on the same post at the exact same millisecond:
1. Thread 1 reads `upvote_count = 10` from the database into Java memory.
2. Thread 2 reads `upvote_count = 10` from the database into Java memory.
3. Thread 1 computes `10 + 1 = 11` and writes `11` back to PostgreSQL.
4. Thread 2 computes `10 + 1 = 11` and writes `11` back to PostgreSQL.
**Result**: 2 real users upvoted, but the counter only increased by 1. One user's upvote is permanently lost in the counter, causing data divergence between the `posts` counter and the `post_reactions` audit log table.

### HOW is it solved?
There are three industry-standard ways to solve this:
1. **Atomic SQL Statements (PostgreSQL level)**: Never calculate new values in application memory if the database can do it atomically.
2. **Optimistic / Pessimistic Locking**: Guard entity state transitions with `@Version` or `SELECT ... FOR UPDATE`.
3. **In-Memory Redis Atomic Operations / Lua Scripts**: Offload high-frequency counters to Redis, where operations are atomic and single-threaded.

---

## 2. Database Atomic Operations vs. Locking

### WHAT are they?
- **Atomic SQL Query**: A single SQL statement that reads and updates the field directly within the database row in one atomic step.
- **Pessimistic Locking (`SELECT FOR UPDATE`)**: Acquires an exclusive database lock on the row before reading, forcing all other threads to block and wait until the transaction commits or rolls back.
- **Optimistic Locking (`@Version`)**: Assumes conflicts are rare. Adds a version counter column. Updates check `WHERE version = :currentVersion`. If another thread changed it first, an `OptimisticLockingFailureException` is thrown.

### WHY choose Atomic SQL over Pessimistic/Optimistic Locks for counters?
- **Pessimistic Locking**: Causes severe thread blocking and lock queueing in PostgreSQL under high traffic (e.g. 500 users voting on a trending post), quickly exhausting the HikariCP database connection pool.
- **Optimistic Locking**: Under high contention, 80%+ of concurrent requests will fail with `OptimisticLockingFailureException` and require complex application retries, wasting CPU.
- **Atomic SQL**: PostgreSQL acquires the lock ONLY for the few microseconds it takes to execute the addition, releasing it immediately.

### HOW does Atomic SQL work in Spring Data JPA?

```java
@Modifying
@Query("UPDATE PostEntity p SET p.upvoteCount = p.upvoteCount + :delta WHERE p.id = :postId")
int adjustUpvoteCount(@Param("postId") Long postId, @Param("delta") int delta);
```

Coupled with an idempotent reaction insert:
```sql
INSERT INTO post_reactions (post_id, user_id, reaction_type, created_at)
VALUES (:postId, :userId, :reactionType, NOW())
ON CONFLICT (post_id, user_id) 
DO UPDATE SET reaction_type = EXCLUDED.reaction_type
WHERE post_reactions.reaction_type != EXCLUDED.reaction_type;
```
- If the user double-clicks rapidly, PostgreSQL resolves the conflict via `ON CONFLICT`, preventing HTTP 500 unique constraint crashes.

---

## 3. Redis In-Memory Atomicity & Lua Scripting

### WHAT is it?
- **Redis**: An in-memory key-value data store whose core execution engine is **single-threaded** (via an event loop). Because commands run sequentially one after another, operations on a single key are inherently thread-safe.
- **Lua Scripting (`EVAL` / `EVALSHA`)**: Allows writing multi-step transactional logic executed directly on the Redis server. Redis guarantees that **no other command or script runs while a Lua script is executing**. It is fully atomic.

### WHY use Redis Lua for Voting & High-Frequency Operations?
When millions of users browse and vote:
1. Going to PostgreSQL for every single vote stresses disk I/O and connection pools.
2. Checking if a user already voted and updating the counter involves 2 operations:
   - Check membership in Set: `SISMEMBER post:{id}:upvoters {userId}`
   - Increment counter in Hash: `HINCRBY post:{id}:stats upvotes 1`
3. If run as two separate Redis commands, a race condition can still occur between command 1 and command 2!
4. Wrapping them in a **Lua script** guarantees that checking the user and updating the counter happens as **one unbreakable atomic unit**.

### HOW does a Redis Lua Script work?

```lua
-- KEYS[1] = "post:{id}:upvoters" (Redis Set of user IDs)
-- KEYS[2] = "post:{id}:stats"    (Redis Hash with counter fields)
-- ARGV[1] = userId

local alreadyVoted = redis.call('SISMEMBER', KEYS[1], ARGV[1])

if alreadyVoted == 1 then
    -- User already voted: toggle OFF (cancel vote)
    redis.call('SREM', KEYS[1], ARGV[1])
    local newCount = redis.call('HINCRBY', KEYS[2], 'upvotes', -1)
    return { 0, newCount } -- 0 = removed
else
    -- User hasn't voted: toggle ON
    redis.call('SADD', KEYS[1], ARGV[1])
    local newCount = redis.call('HINCRBY', KEYS[2], 'upvotes', 1)
    return { 1, newCount } -- 1 = added
end
```

---

## 4. View Count Hot-Spotting: Write-Behind & HyperLogLog

### WHAT is the Problem?
In naive implementations:
```java
post.incrementViewCount();
postRepository.save(post);
```
Every user reading a post executes a database write. At 1,000 views/sec, 1,000 database row-level write locks are taken on the same row, crashing the database.

### WHY use HyperLogLog & Write-Behind?
1. **HyperLogLog (HLL)**: A probabilistic data structure in Redis (`PFADD`, `PFCOUNT`) that estimates the cardinality (unique count) of millions of unique elements using only **12 KB of fixed memory** with a standard error of < 0.81%.
2. **Write-Behind (Write-Back) Pattern**: Writes are applied immediately to fast in-memory cache (Redis) and acknowledged to the client in < 1ms. A background scheduled task periodically flushes the aggregated delta counts to PostgreSQL in bulk.

### HOW does Write-Behind work?
1. Client views post:
   - Backend calls Redis: `PFADD post:101:unique_views user_123`
   - If return is `1` (new unique viewer), backend calls: `INCR post:101:view_delta` and adds `101` to a dirty set: `SADD dirty_posts 101`.
2. Scheduled worker (every 10 seconds):
   - Reads `dirty_posts` set.
   - For each post ID, pops `view_delta` and runs a single batch SQL statement:
     ```sql
     UPDATE posts SET view_count = view_count + :delta WHERE id = :postId;
     ```
   - Drops database write frequency from 1,000 queries/sec down to 1 batch query every 10 seconds!

---

## 5. Java 21 Virtual Threads (Project Loom)

### WHAT are they?
- **Platform Threads (OS Threads)**: 1 Java thread = 1 Operating System kernel thread. Heavy (~1MB stack per thread). Limited by OS memory and CPU context-switching overhead (typically max ~200-500 threads per server).
- **Virtual Threads (Green Threads)**: Lightweight threads managed directly by the Java Virtual Machine (JVM). Each virtual thread consumes only a few hundred bytes. The JVM can effortlessly schedule **millions** of virtual threads on a small pool of carrier OS threads.

### WHY do they matter for Spring Boot?
Spring MVC uses a "thread-per-request" model.
- When an OS thread makes a database query or Redis network call, it enters the **BLOCKED / WAITING** state. The OS thread sits doing zero CPU work while hogging 1MB of RAM. If all 200 Tomcat threads are waiting on the database, the server stops accepting new connections.
- When a **Virtual Thread** blocks on I/O, the JVM automatically **unmounts** it from the underlying carrier thread and assigns another virtual thread to execute. When the I/O response arrives, the JVM resumes the virtual thread.

### HOW do you enable and verify it?
In `application.yaml`:
```yaml
spring:
  threads:
    virtual:
      enabled: true
```
To verify in code:
```java
Thread.currentThread().isVirtual(); // Returns true!
```

---

## 6. Benchmarking & Load Testing with k6

### WHAT is k6?
k6 is a developer-centric, open-source load testing tool written in Go that executes test scenarios scripted in JavaScript. It outputs rich latency percentiles, throughput metrics, and assertion thresholds.

### WHY benchmark in an internship portfolio?
Interviewers want to see **evidence and engineering rigor**, not just claims. A candidate who can say:
> *"I identified a lost-update race condition under concurrent voting. Using k6 with 500 concurrent virtual users, I demonstrated a 38% data loss before optimization, and proved 0.0% data loss with sub-15ms p99 latency after implementing atomic conditional updates and Redis Lua scripts."*

instantly distinguishes themselves as a top-tier candidate.

### HOW does a k6 Concurrency Benchmark work?

```javascript
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    concurrent_voting: {
      executor: 'shared-iterations',
      vus: 100,             // 100 concurrent simulated users
      iterations: 500,      // exactly 500 total votes sent
      maxDuration: '30s',
    },
  },
};

export default function () {
  const payload = JSON.stringify({ reactionType: 'UPVOTE' });
  const res = http.post('http://localhost:8080/api/v1/posts/1/reactions', payload, {
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${__ENV.TOKEN}` },
  });
  check(res, { 'status is 200': (r) => r.status === 200 });
}

// Teardown: Verify that database count matches exactly 500!
export function teardown() {
  const res = http.get('http://localhost:8080/api/v1/posts/1');
  const post = res.json();
  console.log(`Final upvote_count: ${post.upvoteCount} (Expected: 500)`);
}
```

### Key Metrics to Master for Interviews:
- **Throughput (RPS)**: Requests Per Second the server can sustain without dropping packets.
- **p50, p95, p99 Latency**:
  - `p50` = median response time (50% of users experience this or faster).
  - `p95` = 95% of requests are faster than this threshold.
  - `p99` = the tail latency (worst 1% of users). Essential for measuring jitter and lock contention.
- **Error Rate (`http_req_failed`)**: Percentage of requests returning HTTP 4xx or 5xx. Must be 0.00%.

</guide>
