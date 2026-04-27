package org.example.raffle.domain;

import java.time.Instant;

public record RaffleRefund(Long id,
                           String refundId,
                           Long taskId,
                           Long userId,
                           Long strategyId,
                           Long awardId,
                           String refundStatus,
                           String refundMessage,
                           Instant createTime) {
}
