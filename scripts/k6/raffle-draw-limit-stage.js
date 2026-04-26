import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const STRATEGY_ID = Number(__ENV.STRATEGY_ID || '1001');
const START_USER_ID = Number(__ENV.START_USER_ID || '500000');
const USER_STEP = Number(__ENV.USER_STEP || '1');

const RPS = Number(__ENV.RPS || '200');
const DURATION = __ENV.DURATION || '1m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || '200');
const MAX_VUS = Number(__ENV.MAX_VUS || '4000');
const REQ_TIMEOUT = __ENV.REQ_TIMEOUT || '5s';

export const options = {
  scenarios: {
    limit_stage: {
      executor: 'constant-arrival-rate',
      rate: RPS,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS
    }
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
    headers: { 'Content-Type': 'application/json' },
    timeout: REQ_TIMEOUT
  });

  check(response, {
    'status is 200': (r) => r.status === 200,
    'awardId exists': (r) => {
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
