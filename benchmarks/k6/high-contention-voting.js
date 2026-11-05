import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
export const failureRate = new Rate('failed_requests');
export const voteLatency = new Trend('vote_duration', true);
export const successfulVotes = new Counter('successful_votes');

export const options = {
  scenarios: {
    high_contention_votes: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '5s', target: 50 },   // Warm-up ramp to 50 concurrent VUs
        { duration: '15s', target: 100 }, // High contention plateau at 100 VUs
        { duration: '5s', target: 0 },    // Cool down
      ],
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    'http_req_failed': ['rate<0.01'], // Less than 1% failures
    'http_req_duration': ['p(95)<100', 'p(99)<250'], // 95% of requests within 100ms
    'failed_requests': ['rate<0.01'],
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080/api/v1';
const TARGET_POST_ID = __ENV.POST_ID || '1';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';

export default function () {
  const url = `${BASE_URL}/posts/react`;
  const payload = JSON.stringify({
    postId: TARGET_POST_ID,
    type: 'UPVOTE',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      ...(AUTH_TOKEN ? { 'Authorization': `Bearer ${AUTH_TOKEN}` } : {}),
    },
  };

  const res = http.post(url, payload, params);

  const isSuccess = check(res, {
    'status is 200 or 201': (r) => r.status === 200 || r.status === 201,
  });

  voteLatency.add(res.timings.duration);

  if (!isSuccess) {
    failureRate.add(1);
  } else {
    failureRate.add(0);
    successfulVotes.add(1);
  }

  // Small jitter between requests to simulate bursty user interaction
  sleep(0.05);
}
