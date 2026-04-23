package org.example.raffle.domain;

public record StockAssembleResult(Long strategyId,
                                  Long awardId,
                                  int replenishCount,
                                  int dbBefore,
                                  int redisBefore,
                                  int afterStock,
                                  String message) {
}
