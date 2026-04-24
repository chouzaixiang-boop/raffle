package org.example.raffle.domain;

public record ActivityOptionResponse(Long activityId,
                                     String activityName,
                                     Long strategyId) {
}