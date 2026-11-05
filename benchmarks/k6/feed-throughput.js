import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
export const feedDuration = new Trend('feed_req_duration', true);
export const cacheHitRatio = new Rate('cache_estimated_hit_rate');
export const failedRequests = new Rate('failed_requests');

export const options = {
  scenarios: {
    feed_high_load: {
      executor: 'constant-vus',
      vus: 50,
      duration: '30s',
    },
  },
  thresholds: {
    'http_req_failed': ['rate<0.01'], // <1% failure
    'http_req_duration': ['p(95)<30', 'p(99)<60'], // Cache hits should comfortably serve in sub-30ms p95
    'failed_requests': ['rate<0.01'],
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080/api/v1';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';

export default function () {
  // Test hot feed and general feed
  const url = `${BASE_URL}/feed?page=0&size=20`;
  const params = {
    headers: {
      'Content-Type': 'application/json',
      ...(AUTH_TOKEN ? { 'Authorization': `Bearer ${AUTH_TOKEN}` } : {}),
    },
  };

  const res = http.get(url, params);

  const isOk = check(res, {
    'status is 200': (r) => r.status === 200,
  });

  feedDuration.add(res.timings.duration);

  if (!isOk) {
    failedRequests.add(1);
  } else {
    failedRequests.add(0);
    // Latency under 25ms strongly indicates Redis cache hit
    cacheHitRatio.add(res.timings.duration < 25 ? 1 : 0);
  }

  sleep(0.02);
}
