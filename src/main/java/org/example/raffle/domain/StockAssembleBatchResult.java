package org.example.raffle.domain;

public record StockAssembleBatchResult(Long strategyId,
                                       Long awardId,
                                       int replenishCount,
                                       boolean success,
                                       Integer dbBefore,
                                       Integer redisBefore,
                                       Integer afterStock,
                                       String message) {
}
