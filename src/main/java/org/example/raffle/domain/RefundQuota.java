package org.example.raffle.domain;

public record RefundQuota(Long userId,
                          Long strategyId,
                          int usedCount,
                          int maxCount,
                          int version) {
}
