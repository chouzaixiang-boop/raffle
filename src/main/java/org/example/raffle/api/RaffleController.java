package org.example.raffle.api;

import org.example.raffle.domain.ActivityOptionResponse;
import org.example.raffle.domain.AwardTask;
import org.example.raffle.domain.RaffleResult;
import org.example.raffle.domain.ActivityPageResponse;
import org.example.raffle.domain.StockAssembleBatchResult;
import org.example.raffle.domain.StockAssembleCommand;
import org.example.raffle.domain.StockAssembleResult;
import org.example.raffle.service.RaffleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/raffle")
public class RaffleController {

    private final RaffleService raffleService;

    public RaffleController(RaffleService raffleService) {
        this.raffleService = raffleService;
    }

    @PostMapping("/draw")
    public DrawResponse draw(@RequestBody DrawRequest request) {
        if (request == null || request.userId() == null || request.strategyId() == null) {
            throw new IllegalArgumentException("userId and strategyId are required");
        }
        RaffleResult result = raffleService.draw(request.userId(), request.strategyId());
        return new DrawResponse(result.userId(), result.strategyId(), result.awardId(), result.awardName(), result.taskId(), result.success(), result.message());
    }

    @GetMapping("/activities/{activityId}")
    public ActivityPageResponse getActivityPage(@PathVariable Long activityId) {
        if (activityId == null) {
            throw new IllegalArgumentException("activityId is required");
        }
        return raffleService.getActivityPage(activityId);
    }

    @GetMapping("/activities")
    public List<ActivityOptionResponse> listActivities() {
        return raffleService.listActivities();
    }

    @GetMapping("/tasks/{taskId}")
    public AwardTask getAwardTask(@PathVariable Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId is required");
        }
        return raffleService.getAwardTask(taskId);
    }

    @PostMapping("/assemble/stock")
    public StockAssembleResponse assembleStock(@RequestBody StockAssembleRequest request) {
        if (request == null || request.strategyId() == null || request.awardId() == null || request.replenishCount() == null) {
            throw new IllegalArgumentException("strategyId, awardId and replenishCount are required");
        }
        StockAssembleResult result = raffleService.assembleStock(request.strategyId(), request.awardId(), request.replenishCount());
        return new StockAssembleResponse(
                result.strategyId(),
                result.awardId(),
                result.replenishCount(),
                result.dbBefore(),
                result.redisBefore(),
                result.afterStock(),
                result.message()
        );
    }

            @PostMapping("/assemble/stock/batch")
            public BatchStockAssembleResponse assembleStockBatch(@RequestBody BatchStockAssembleRequest request) {
            if (request == null || request.items() == null || request.items().isEmpty()) {
                throw new IllegalArgumentException("items are required");
            }
            List<StockAssembleCommand> commands = request.items()
                .stream()
                .map(item -> new StockAssembleCommand(
                    item == null ? null : item.strategyId(),
                    item == null ? null : item.awardId(),
                    item == null || item.replenishCount() == null ? 0 : item.replenishCount()
                ))
                .toList();
            List<StockAssembleBatchResult> results = raffleService.assembleStockBatch(commands);
            List<StockAssembleBatchItemResponse> responseItems = results.stream()
                .map(item -> new StockAssembleBatchItemResponse(
                    item.strategyId(),
                    item.awardId(),
                    item.replenishCount(),
                    item.success(),
                    item.dbBefore(),
                    item.redisBefore(),
                    item.afterStock(),
                    item.message()
                ))
                .toList();
            int successCount = (int) results.stream().filter(StockAssembleBatchResult::success).count();
            return new BatchStockAssembleResponse(results.size(), successCount, results.size() - successCount, responseItems);
            }
}
