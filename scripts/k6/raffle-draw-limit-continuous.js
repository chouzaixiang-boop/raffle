import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const STRATEGY_ID = Number(__ENV.STRATEGY_ID || '1001');
const START_USER_ID = Number(__ENV.START_USER_ID || '900000');
const USER_STEP = Number(__ENV.USER_STEP || '1');

const START_RPS = Number(__ENV.START_RPS || '50');
const RPS_LADDER = (__ENV.RPS_LADDER || '100,200,300,400,500,600,700,800,900,1000').split(',').map((s) => Number(s.trim())).filter((n) => Number.isFinite(n) && n > 0);
const STEP_DURATION = __ENV.STEP_DURATION || '3m';
const HOLD_DURATION = __ENV.HOLD_DURATION || '24h';

const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || '800');
const MAX_VUS = Number(__ENV.MAX_VUS || '8000');
const REQ_TIMEOUT = __ENV.REQ_TIMEOUT || '5s';

function buildStages() {
  const stages = [];
  for (const target of RPS_LADDER) {
    stages.push({ target, duration: STEP_DURATION });
  }
  if (RPS_LADDER.length > 0) {
    stages.push({ target: RPS_LADDER[RPS_LADDER.length - 1], duration: HOLD_DURATION });
  } else {
    stages.push({ target: START_RPS, duration: HOLD_DURATION });
  }
  return stages;
}

export const options = {
  discardResponseBodies: true,
  scenarios: {
    limit_continuous: {
      executor: 'ramping-arrival-rate',
      startRate: START_RPS,
      timeUnit: '1s',
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
      stages: buildStages(),
      gracefulStop: '0s'
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
    'status is 200': (r) => r.status === 200
  });
}
