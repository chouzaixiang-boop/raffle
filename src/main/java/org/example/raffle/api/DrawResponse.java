package org.example.raffle.api;

public record DrawResponse(Long userId,
                           Long strategyId,
                           Long awardId,
                           String awardName,
                           boolean success,
                           String message) {
}
