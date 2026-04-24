import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const STRATEGY_ID = Number(__ENV.STRATEGY_ID || '1001');
const START_USER_ID = Number(__ENV.START_USER_ID || '500000');
const USER_STEP = Number(__ENV.USER_STEP || '1');

const RPS = Number(__ENV.RPS || '200');
const DURATION = __ENV.DURATION || '1m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || '100');
const MAX_VUS = Number(__ENV.MAX_VUS || '3000');

const drawDurationMs = new Trend('raffle_draw_duration_ms', true);
const drawFailedRate = new Rate('raffle_draw_failed_rate');

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
    headers: { 'Content-Type': 'application/json' }
  });

  const ok = check(response, {
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

  drawDurationMs.add(response.timings.duration);
  drawFailedRate.add(!ok);

  sleep(0.005);
}