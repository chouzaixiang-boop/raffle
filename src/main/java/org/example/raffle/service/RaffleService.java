package org.example.raffle.service;

import org.example.raffle.domain.RaffleResult;
import org.example.raffle.domain.StockAssembleBatchResult;
import org.example.raffle.domain.StockAssembleCommand;
import org.example.raffle.domain.StockAssembleResult;

import java.util.List;

public interface RaffleService {

    RaffleResult draw(Long userId, Long strategyId);

    StockAssembleResult assembleStock(Long strategyId, Long awardId, int replenishCount);

    List<StockAssembleBatchResult> assembleStockBatch(List<StockAssembleCommand> commands);
}
