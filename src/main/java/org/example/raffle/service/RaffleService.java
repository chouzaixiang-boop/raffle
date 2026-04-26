package org.example.raffle.service;

import org.example.raffle.domain.ActivityPageResponse;
import org.example.raffle.domain.ActivityOptionResponse;
import org.example.raffle.domain.AwardTask;
import org.example.raffle.domain.RaffleResult;
import org.example.raffle.domain.StockAssembleBatchResult;
import org.example.raffle.domain.StockAssembleCommand;
import org.example.raffle.domain.StockAssembleResult;

import java.util.List;

public interface RaffleService {

    RaffleResult draw(Long userId, Long strategyId);

    ActivityPageResponse getActivityPage(Long activityId);

    List<ActivityOptionResponse> listActivities();

    StockAssembleResult assembleStock(Long strategyId, Long awardId, int replenishCount);

    List<StockAssembleBatchResult> assembleStockBatch(List<StockAssembleCommand> commands);

    AwardTask getAwardTask(Long taskId);
}
