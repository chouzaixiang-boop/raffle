package org.example.raffle.domain;

public record RaffleResult(Long userId,
                           Long strategyId,
                           Long awardId,
                           String awardName,
                           Long taskId,
                           boolean success,
                           String message) {
}
