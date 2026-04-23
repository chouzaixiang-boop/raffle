package org.example.raffle.api;

public record StockAssembleResponse(Long strategyId,
                                    Long awardId,
                                    Integer replenishCount,
                                    Integer dbBefore,
                                    Integer redisBefore,
                                    Integer afterStock,
                                    String message) {
}
