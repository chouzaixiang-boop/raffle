package org.example.raffle.api;

public record RefundQuotaResponse(Long userId,
                                  Long strategyId,
                                  Integer usedCount,
                                  Integer maxCount,
                                  Integer remainingCount) {
}
