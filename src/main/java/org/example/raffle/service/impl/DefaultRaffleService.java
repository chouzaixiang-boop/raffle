package org.example.raffle.service.impl;

import org.example.raffle.domain.Award;
import org.example.raffle.domain.RaffleContext;
import org.example.raffle.domain.RaffleRecord;
import org.example.raffle.domain.RaffleResult;
import org.example.raffle.domain.StockAssembleBatchResult;
import org.example.raffle.domain.StockAssembleCommand;
import org.example.raffle.domain.StockAssembleResult;
import org.example.raffle.domain.Strategy;
import org.example.raffle.domain.StrategyAward;
import org.example.raffle.repository.AwardRepository;
import org.example.raffle.repository.RaffleRecordRepository;
import org.example.raffle.repository.RuleRepository;
import org.example.raffle.repository.StrategyAwardRepository;
import org.example.raffle.repository.StrategyRepository;
import org.example.raffle.rule.RuleHandler;
import org.example.raffle.rule.RuleHandlerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.example.raffle.service.RaffleService;
import org.example.raffle.service.RaffleStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class DefaultRaffleService implements RaffleService {

    private static final Logger log = LoggerFactory.getLogger(DefaultRaffleService.class);

    private final StrategyRepository strategyRepository;
    private final StrategyAwardRepository strategyAwardRepository;
    private final AwardRepository awardRepository;
    private final RuleRepository ruleRepository;
    private final RaffleRecordRepository raffleRecordRepository;
    private final RuleHandlerRegistry ruleHandlerRegistry;
    private final RaffleStateService raffleStateService;
    private final Timer persistRecordTimer;
    private final Random random = new Random();

    public DefaultRaffleService(StrategyRepository strategyRepository,
                                StrategyAwardRepository strategyAwardRepository,
                                AwardRepository awardRepository,
                                RuleRepository ruleRepository,
                                RaffleRecordRepository raffleRecordRepository,
                                RuleHandlerRegistry ruleHandlerRegistry,
                                RaffleStateService raffleStateService,
                                MeterRegistry meterRegistry) {
        this.strategyRepository = strategyRepository;
        this.strategyAwardRepository = strategyAwardRepository;
        this.awardRepository = awardRepository;
        this.ruleRepository = ruleRepository;
        this.raffleRecordRepository = raffleRecordRepository;
        this.ruleHandlerRegistry = ruleHandlerRegistry;
        this.raffleStateService = raffleStateService;
        this.persistRecordTimer = Timer.builder("raffle.persist.record.duration")
                .description("Persist raffle record duration")
                .register(meterRegistry);
    }

    @Override
    public RaffleResult draw(Long userId, Long strategyId) {
        StopWatch stopWatch = new StopWatch("raffle-draw");

        stopWatch.start("loadStrategy");
        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new IllegalArgumentException("strategy not found: " + strategyId));
        stopWatch.stop();

        RaffleContext context = new RaffleContext(userId, strategyId);

        stopWatch.start("preRules");
        applyRules(strategy.ruleModels(), context);
        stopWatch.stop();

        if (context.getActualStrategyId() == 0L) {
            stopWatch.start("fallbackFinalize");
            finalizeAsConsolation(context);
            stopWatch.stop();

            stopWatch.start("persistRecord");
            RaffleResult result = persistWithMetrics(context);
            stopWatch.stop();

            logDrawTiming(userId, strategyId, context, stopWatch);
            return result;
        }

        stopWatch.start("selectAward");
        StrategyAward selectedAward = selectAward(context.getActualStrategyId());
        stopWatch.stop();

        context.setAwardId(selectedAward.awardId());
        context.setAwardName(awardRepository.findById(selectedAward.awardId())
                .map(award -> award.awardName())
                .orElse(selectedAward.awardTitle()));

        stopWatch.start("postRules");
        applyRules(strategyAwardRepository.findByStrategyIdAndAwardId(context.getActualStrategyId(), context.getAwardId()).ruleModels(), context);
        stopWatch.stop();

        if (!context.isSuccess()) {
            stopWatch.start("fallbackFinalize");
            finalizeAsConsolation(context);
            stopWatch.stop();
        }

        if (context.isSuccess()) {
            stopWatch.start("incrementUserCount");
            raffleStateService.incrementUserCount(userId, strategyId);
            stopWatch.stop();
        }

        stopWatch.start("persistRecord");
        RaffleResult result = persistWithMetrics(context);
        stopWatch.stop();

        logDrawTiming(userId, strategyId, context, stopWatch);
        return result;
    }

    @Override
    public StockAssembleResult assembleStock(Long strategyId, Long awardId, int replenishCount) {
        if (replenishCount <= 0) {
            throw new IllegalArgumentException("replenishCount must be greater than 0");
        }
        StrategyAward currentConfig = strategyAwardRepository.findByStrategyIdAndAwardId(strategyId, awardId);
        int dbBefore = currentConfig.awardSurplus();
        int redisBefore = raffleStateService.getStock(strategyId, awardId);
        int baseStock = Math.max(dbBefore, redisBefore);
        int afterStock = baseStock + replenishCount;

        strategyAwardRepository.updateSurplus(strategyId, awardId, afterStock);
        raffleStateService.setStock(strategyId, awardId, afterStock);

        return new StockAssembleResult(strategyId, awardId, replenishCount, dbBefore, redisBefore, afterStock, "assembled and synchronized");
    }

    @Override
    public List<StockAssembleBatchResult> assembleStockBatch(List<StockAssembleCommand> commands) {
        List<StockAssembleBatchResult> results = new ArrayList<>();
        for (StockAssembleCommand command : commands) {
            if (command == null || command.strategyId() == null || command.awardId() == null || command.replenishCount() <= 0) {
                results.add(new StockAssembleBatchResult(
                        command == null ? null : command.strategyId(),
                        command == null ? null : command.awardId(),
                        command == null ? 0 : command.replenishCount(),
                        false,
                        null,
                        null,
                        null,
                        "invalid item: strategyId, awardId and replenishCount>0 are required"
                ));
                continue;
            }
            try {
                StockAssembleResult single = assembleStock(command.strategyId(), command.awardId(), command.replenishCount());
                results.add(new StockAssembleBatchResult(
                        single.strategyId(),
                        single.awardId(),
                        single.replenishCount(),
                        true,
                        single.dbBefore(),
                        single.redisBefore(),
                        single.afterStock(),
                        single.message()
                ));
            } catch (Exception ex) {
                results.add(new StockAssembleBatchResult(
                        command.strategyId(),
                        command.awardId(),
                        command.replenishCount(),
                        false,
                        null,
                        null,
                        null,
                        ex.getMessage()
                ));
            }
        }
        return results;
    }

    private void applyRules(List<String> ruleModels, RaffleContext context) {
        for (String ruleModel : ruleModels) {
            long ruleStartTime = System.nanoTime();
            RuleHandler ruleHandler = ruleHandlerRegistry.getHandler(ruleModel);
            ruleHandler.apply(context);
            long ruleCostMillis = (System.nanoTime() - ruleStartTime) / 1_000_000;
            if (log.isDebugEnabled()) {
                log.debug(
                        "raffle_rule_timing userId={} strategyId={} actualStrategyId={} awardId={} ruleModel={} costMs={} success={} message={}",
                        context.getUserId(),
                        context.getStrategyId(),
                        context.getActualStrategyId(),
                        context.getAwardId(),
                        ruleModel,
                        ruleCostMillis,
                        context.isSuccess(),
                        context.getMessage()
                );
            }
            if (!context.isSuccess()) {
                break;
            }
        }
    }

    private StrategyAward selectAward(Long strategyId) {
        List<StrategyAward> awards = strategyAwardRepository.findByStrategyId(strategyId);
        List<StrategyAward> eligibleAwards = new ArrayList<>();
        BigDecimal totalRate = BigDecimal.ZERO;
        for (StrategyAward award : awards) {
            if (award.awardId().equals(raffleStateService.getConsolationAwardId())) {
                continue;
            }
            eligibleAwards.add(award);
            totalRate = totalRate.add(award.awardRate());
        }
        if (eligibleAwards.isEmpty()) {
            throw new IllegalArgumentException("no awards configured for strategy: " + strategyId);
        }
        BigDecimal randomRate = BigDecimal.valueOf(random.nextDouble()).multiply(totalRate);
        BigDecimal cursor = BigDecimal.ZERO;
        for (StrategyAward award : eligibleAwards) {
            cursor = cursor.add(award.awardRate());
            if (randomRate.compareTo(cursor) < 0) {
                return award;
            }
        }
        return eligibleAwards.get(eligibleAwards.size() - 1);
    }

    private void finalizeAsConsolation(RaffleContext context) {
        context.setAwardId(raffleStateService.getConsolationAwardId());
        context.setAwardName(raffleStateService.getAwardName(context.getAwardId()));
    }

    private RaffleResult persist(RaffleContext context) {
        RaffleResult result = new RaffleResult(context.getUserId(), context.getStrategyId(), context.getAwardId(), context.getAwardName(), context.isSuccess(), context.getMessage());
        raffleRecordRepository.save(new RaffleRecord(context.getUserId(), context.getStrategyId(), context.getAwardId(), context.getAwardName(), context.isSuccess(), context.getMessage(), Instant.now()));
        return result;
    }

    private RaffleResult persistWithMetrics(RaffleContext context) {
        Timer.Sample sample = Timer.start();
        try {
            return persist(context);
        } finally {
            sample.stop(persistRecordTimer);
        }
    }

    private void logDrawTiming(Long userId, Long strategyId, RaffleContext context, StopWatch stopWatch) {
        if (!log.isInfoEnabled()) {
            return;
        }
        StringBuilder stageMetrics = new StringBuilder();
        for (StopWatch.TaskInfo taskInfo : stopWatch.getTaskInfo()) {
            if (stageMetrics.length() > 0) {
                stageMetrics.append(',');
            }
            stageMetrics.append(taskInfo.getTaskName()).append('=').append(taskInfo.getTimeMillis());
        }
        log.info(
                "raffle_draw_timing userId={} strategyId={} awardId={} success={} totalMs={} stageMs={}",
                userId,
                strategyId,
                context.getAwardId(),
                context.isSuccess(),
                stopWatch.getTotalTimeMillis(),
                stageMetrics
        );
    }
}