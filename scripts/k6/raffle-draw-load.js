import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const STRATEGY_ID = Number(__ENV.STRATEGY_ID || '1001');
const START_USER_ID = Number(__ENV.START_USER_ID || '200000');
const USER_STEP = Number(__ENV.USER_STEP || '1');

const RPS = Number(__ENV.RPS || '800');
const DURATION = __ENV.DURATION || '5m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || '300');
const MAX_VUS = Number(__ENV.MAX_VUS || '2000');

export const options = {
  scenarios: {
    steady: {
      executor: 'constant-arrival-rate',
      rate: RPS,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS
    }
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<200', 'p(99)<500']
  }
};

function buildUserId() {
  return START_USER_ID + (__VU * 1000000) + (__ITER * USER_STEP);
}

export default function () {
  const payload = JSON.stringify({
    userId: buildUserId(),
    strategyId: STRATEGY_ID
  });

  const response = http.post(`${BASE_URL}/api/raffle/draw`, payload, {
    headers: { 'Content-Type': 'application/json' }
  });

  check(response, {
    'status is 200': (r) => r.status === 200,
    'response has awardId': (r) => {
      try {
        const body = r.json();
        return body && body.awardId !== undefined;
      } catch (_) {
        return false;
      }
    }
  });

  sleep(0.01);
}
