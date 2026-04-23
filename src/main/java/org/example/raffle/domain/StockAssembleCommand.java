package org.example.raffle.domain;

public record StockAssembleCommand(Long strategyId, Long awardId, int replenishCount) {
}
