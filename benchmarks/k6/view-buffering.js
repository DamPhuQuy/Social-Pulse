import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

export const viewDuration = new Trend('view_req_duration', true);
export const fastResponses = new Rate('sub_10ms_responses');
export const failedRequests = new Rate('failed_requests');

export const options = {
  scenarios: {
    view_burst: {
      executor: 'shared-iterations',
      vus: 50,
      iterations: 5000,
      maxDuration: '30s',
    },
  },
  thresholds: {
    'http_req_failed': ['rate<0.01'],
    'http_req_duration': ['p(95)<25', 'p(99)<50'], // Non-blocking async view buffer should be very fast
    'sub_10ms_responses': ['rate>0.80'],          // >80% sub-10ms responses expected
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080/api/v1';
const TARGET_POST_ID = __ENV.POST_ID || '1';

export default function () {
  // Simulate distinct viewers by passing random simulated user ID in headers
  const randomUserId = Math.floor(Math.random() * 1000) + 1;
  const url = `${BASE_URL}/posts/${TARGET_POST_ID}/view`;

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Simulated-User-Id': String(randomUserId),
    },
  };

  const res = http.post(url, null, params);

  const isOk = check(res, {
    'status is 200 or 204': (r) => r.status === 200 || r.status === 204,
  });

  viewDuration.add(res.timings.duration);

  if (!isOk) {
    failedRequests.add(1);
  } else {
    failedRequests.add(0);
    fastResponses.add(res.timings.duration < 10 ? 1 : 0);
  }

  sleep(0.005);
}
