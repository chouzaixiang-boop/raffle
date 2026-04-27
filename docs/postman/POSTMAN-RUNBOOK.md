# Postman Staged Runbook

## Files

- Collection: docs/postman/raffle-stage-tests.postman_collection.json
- Environment: docs/postman/raffle-local.postman_environment.json

## Import and Run

1. Import both files into Postman.
2. Select environment raffle-local.
3. Run requests in order:
   - A1 Draw Once
   - A2 Query Task
   - B1 Wait and Query Task Again
   - C1 Query Refund Quota
   - C2 Apply Refund (Generate refundId)
   - C3 Apply Refund Idempotent Replay
   - D1 Metrics Prometheus

## Assertions by Stage

### Stage A: Draw

- Draw response has success and taskId.
- taskId is written into collection variable taskId.

### Stage B: Award Async

- Task status should converge to one of AWARDED / REFUNDED / FAILED.
- If status remains PENDING or PROCESSING for long, check compensation metrics and logs.

### Stage C: Refund

- Refund apply endpoint is /api/raffle/refund/apply.
- First request validates accepted and refundStatus fields.
- Replay with same refundId validates idempotent behavior.

### Stage D: Compensation and Monitoring

- Actuator endpoint /actuator/prometheus returns 200.
- Metrics should include:
  - raffle_award_compensation_scan_total
  - raffle_award_stream_pending_count

## Troubleshooting

- taskId missing:
  - Ensure A1 returns success true.
  - Re-run A1 then A2.
- refund rejected due to status:
  - Wait until task reaches AWARDED, then run C2.
- metrics not found:
  - Verify management endpoint exposure and active profile config in application.properties.
