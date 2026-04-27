package org.example.raffle.repository;

import org.example.raffle.domain.RefundQuota;

import java.util.Optional;

public interface RefundQuotaRepository {

    void initializeIfAbsent(Long userId, Long strategyId, int maxCount);

    Optional<RefundQuota> findByUserAndStrategy(Long userId, Long strategyId);

    int incrementUsedWithVersion(Long userId, Long strategyId, int currentVersion);
}
