# DevPulse k6 High-Concurrency Benchmark Suite

This directory contains containerized k6 load tests designed to benchmark DevPulse's high-concurrency architecture.

## Scenarios

| Script | Objective | What It Tests |
|---|---|---|
| `high-contention-voting.js` | 100 concurrent VUs hammering `/posts/react` on a single post | Confirms zero lost updates, atomic SQL execution, p95 < 100ms |
| `feed-throughput.js` | 50 concurrent VUs querying `/feed` | Tests Redis timeline caching, cache hit latency (<15ms), stampede resistance |
| `view-buffering.js` | 5,000 views burst against `/posts/{id}/view` | Tests Redis HyperLogLog view deduplication and non-blocking write-behind latency |

## Running via Docker (No Local k6 Installation Required)

```bash
# 1. High Contention Voting Test
docker run --rm -i --network=host -v $(pwd):/scripts grafana/k6 run /scripts/high-contention-voting.js

# 2. Feed Throughput & Cache Performance Test
docker run --rm -i --network=host -v $(pwd):/scripts grafana/k6 run /scripts/feed-throughput.js

# 3. View Buffering & HyperLogLog Burst Test
docker run --rm -i --network=host -v $(pwd):/scripts grafana/k6 run /scripts/view-buffering.js
```

## Running via Docker Compose

Add the `k6` service to `docker-compose.yaml` or run on-demand:

```bash
docker compose run --rm --entrypoint k6 k6 run /scripts/high-contention-voting.js
```
