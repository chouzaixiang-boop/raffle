package org.example.raffle.service.impl;

import org.example.raffle.cache.RedisStaticDataCache;
import org.example.raffle.api.RefundApplyResponse;
import org.example.raffle.domain.AwardTask;
import org.example.raffle.domain.RaffleRefund;
import org.example.raffle.domain.RefundQuota;
import org.example.raffle.domain.StrategyAward;
import org.example.raffle.repository.AwardTaskRepository;
import org.example.raffle.repository.RaffleRefundRepository;
import org.example.raffle.repository.RefundQuotaRepository;
import org.example.raffle.repository.StrategyAwardRepository;
import org.example.raffle.service.RaffleStateService;
import org.example.raffle.service.RefundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Profile("!local")
public class RedisRefundService implements RefundService {

    private static final Logger log = LoggerFactory.getLogger(RedisRefundService.class);

    private static final String TASK_STATUS_AWARDED = "AWARDED";
    private static final String TASK_STATUS_REFUNDED = "REFUNDED";
    private static final String REFUND_STATUS_REFUNDED = "REFUNDED";
    private static final int DEFAULT_MAX_REFUND_COUNT = 3;
    private static final String IDEMPOTENT_KEY_PREFIX = "raffle:refund:idempotent:";

    private final StringRedisTemplate redisTemplate;
    private final AwardTaskRepository awardTaskRepository;
    private final RaffleRefundRepository raffleRefundRepository;
    private final RefundQuotaRepository refundQuotaRepository;
    private final StrategyAwardRepository strategyAwardRepository;
    private final RaffleStateService raffleStateService;
    private final RedisStaticDataCache cache;

    public RedisRefundService(StringRedisTemplate redisTemplate,
                              AwardTaskRepository awardTaskRepository,
                              RaffleRefundRepository raffleRefundRepository,
                              RefundQuotaRepository refundQuotaRepository,
                              StrategyAwardRepository strategyAwardRepository,
                              RaffleStateService raffleStateService,
                              RedisStaticDataCache cache) {
        this.redisTemplate = redisTemplate;
        this.awardTaskRepository = awardTaskRepository;
        this.raffleRefundRepository = raffleRefundRepository;
        this.refundQuotaRepository = refundQuotaRepository;
        this.strategyAwardRepository = strategyAwardRepository;
        this.raffleStateService = raffleStateService;
        this.cache = cache;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundApplyResponse applyRefund(String refundId, Long userId, Long taskId) {
        if (userId == null || taskId == null) {
            throw new IllegalArgumentException("userId and taskId are required");
        }
        String effectiveRefundId = (refundId == null || refundId.isBlank())
                ? UUID.randomUUID().toString()
                : refundId;

        RaffleRefund existingByRefundId = raffleRefundRepository.findByRefundId(effectiveRefundId).orElse(null);
        if (existingByRefundId != null) {
            RefundQuota quota = refundQuotaRepository.findByUserAndStrategy(existingByRefundId.userId(), existingByRefundId.strategyId())
                    .orElse(new RefundQuota(existingByRefundId.userId(), existingByRefundId.strategyId(), 0, DEFAULT_MAX_REFUND_COUNT, 0));
            return toAcceptedResponse(existingByRefundId, quota);
        }

        String key = IDEMPOTENT_KEY_PREFIX + effectiveRefundId;
        Boolean lock = redisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(lock)) {
            RaffleRefund existing = raffleRefundRepository.findByRefundId(effectiveRefundId).orElse(null);
            if (existing != null) {
                RefundQuota quota = refundQuotaRepository.findByUserAndStrategy(existing.userId(), existing.strategyId())
                        .orElse(new RefundQuota(existing.userId(), existing.strategyId(), 0, DEFAULT_MAX_REFUND_COUNT, 0));
                return toAcceptedResponse(existing, quota);
            }
            return new RefundApplyResponse(effectiveRefundId, taskId, false, "REJECTED", "duplicate refund request in progress", null, null, null);
        }

        try {
            AwardTask task = awardTaskRepository.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("award task not found: " + taskId));
            if (!userId.equals(task.userId())) {
                throw new IllegalArgumentException("task does not belong to user: " + userId);
            }

            RaffleRefund existingByTask = raffleRefundRepository.findByTaskId(taskId).orElse(null);
            if (existingByTask != null) {
                RefundQuota quota = refundQuotaRepository.findByUserAndStrategy(existingByTask.userId(), existingByTask.strategyId())
                        .orElse(new RefundQuota(existingByTask.userId(), existingByTask.strategyId(), 0, DEFAULT_MAX_REFUND_COUNT, 0));
                return toAcceptedResponse(existingByTask, quota);
            }

            if (!TASK_STATUS_AWARDED.equals(task.taskStatus())) {
                redisTemplate.delete(key);
                return new RefundApplyResponse(effectiveRefundId, taskId, false, "REJECTED", "task is not in AWARDED status", null, null, null);
            }

            refundQuotaRepository.initializeIfAbsent(userId, task.strategyId(), DEFAULT_MAX_REFUND_COUNT);
            RefundQuota quota = consumeQuota(userId, task.strategyId());
            if (quota.usedCount() >= quota.maxCount()) {
                redisTemplate.delete(key);
                return new RefundApplyResponse(effectiveRefundId, taskId, false, "QUOTA_EXCEEDED", "refund quota exceeded", quota.usedCount(), quota.maxCount(), 0);
            }

            int taskUpdated = awardTaskRepository.updateStatus(task.taskId(), TASK_STATUS_AWARDED, TASK_STATUS_REFUNDED, task.version());
            if (taskUpdated <= 0) {
                AwardTask latest = awardTaskRepository.findById(task.taskId()).orElse(task);
                if (TASK_STATUS_REFUNDED.equals(latest.taskStatus())) {
                    RaffleRefund completed = raffleRefundRepository.findByTaskId(task.taskId()).orElse(null);
                    if (completed != null) {
                        RefundQuota latestQuota = refundQuotaRepository.findByUserAndStrategy(userId, task.strategyId()).orElse(quota);
                        return toAcceptedResponse(completed, latestQuota);
                    }
                }
                throw new IllegalStateException("failed to update award task to REFUNDED: " + task.taskId());
            }

            StrategyAward award = strategyAwardRepository.findByStrategyIdAndAwardId(task.strategyId(), task.awardId());
            strategyAwardRepository.increaseSurplusWithCap(task.strategyId(), task.awardId(), 1);

            RaffleRefund refund = raffleRefundRepository.save(new RaffleRefund(
                    null,
                    effectiveRefundId,
                    taskId,
                    userId,
                    task.strategyId(),
                    task.awardId(),
                    REFUND_STATUS_REFUNDED,
                    "refund succeeded, stock replenished",
                    Instant.now()
            ));
            registerAfterCommitReplenish(task.strategyId(), task.awardId(), award.awardAllocate());
            RefundQuota latest = refundQuotaRepository.findByUserAndStrategy(userId, task.strategyId()).orElse(quota);
            return toAcceptedResponse(refund, latest);
        } catch (RuntimeException ex) {
            redisTemplate.delete(key);
            throw ex;
        }
    }

    @Override
    public RefundQuota getQuota(Long userId, Long strategyId) {
        if (userId == null || strategyId == null) {
            throw new IllegalArgumentException("userId and strategyId are required");
        }
        refundQuotaRepository.initializeIfAbsent(userId, strategyId, DEFAULT_MAX_REFUND_COUNT);
        return refundQuotaRepository.findByUserAndStrategy(userId, strategyId)
                .orElse(new RefundQuota(userId, strategyId, 0, DEFAULT_MAX_REFUND_COUNT, 0));
    }

    private RefundQuota consumeQuota(Long userId, Long strategyId) {
        for (int i = 0; i < 3; i++) {
            RefundQuota quota = refundQuotaRepository.findByUserAndStrategy(userId, strategyId)
                    .orElse(new RefundQuota(userId, strategyId, 0, DEFAULT_MAX_REFUND_COUNT, 0));
            if (quota.usedCount() >= quota.maxCount()) {
                return quota;
            }
            int updated = refundQuotaRepository.incrementUsedWithVersion(userId, strategyId, quota.version());
            if (updated > 0) {
                return refundQuotaRepository.findByUserAndStrategy(userId, strategyId).orElse(quota);
            }
        }
        return refundQuotaRepository.findByUserAndStrategy(userId, strategyId)
                .orElse(new RefundQuota(userId, strategyId, DEFAULT_MAX_REFUND_COUNT, DEFAULT_MAX_REFUND_COUNT, 0));
    }

    private RefundApplyResponse toAcceptedResponse(RaffleRefund refund, RefundQuota quota) {
        int remaining = Math.max(quota.maxCount() - quota.usedCount(), 0);
        return new RefundApplyResponse(
                refund.refundId(),
                refund.taskId(),
                true,
                refund.refundStatus(),
                refund.refundMessage(),
                quota.usedCount(),
                quota.maxCount(),
                remaining
        );
    }

    private void registerAfterCommitReplenish(Long strategyId, Long awardId, int maxStock) {
        Runnable action = () -> {
            try {
                int redisAfter = raffleStateService.increaseStock(strategyId, awardId, maxStock);
                cache.updateStrategyAwardSurplus(strategyId, awardId, redisAfter);
            } catch (Exception ex) {
                log.error("refund_replenish_after_commit_failed strategyId={} awardId={} error={}", strategyId, awardId, ex.getMessage(), ex);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
