package org.example.raffle.api;

public record StockAssembleRequest(Long strategyId, Long awardId, Integer replenishCount) {
}
