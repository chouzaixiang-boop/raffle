package org.example.raffle.api;

public record StockAssembleBatchItemResponse(Long strategyId,
                                             Long awardId,
                                             Integer replenishCount,
                                             Boolean success,
                                             Integer dbBefore,
                                             Integer redisBefore,
                                             Integer afterStock,
                                             String message) {
}
