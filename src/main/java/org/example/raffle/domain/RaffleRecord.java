package org.example.raffle.domain;

import java.time.Instant;

public record RaffleRecord(Long userId,
                           Long strategyId,
                           Long awardId,
                           String awardName,
                           boolean success,
                           String message,
                           Instant createTime) {
}
