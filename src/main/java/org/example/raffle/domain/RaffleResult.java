package org.example.raffle.domain;

public record RaffleResult(Long userId,
                           Long strategyId,
                           Long awardId,
                           String awardName,
                           boolean success,
                           String message) {
}
