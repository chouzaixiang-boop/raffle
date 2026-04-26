package org.example.raffle.api;

public record DrawResponse(Long userId,
                           Long strategyId,
                           Long awardId,
                           String awardName,
                           Long taskId,
                           boolean success,
                           String message) {
}
