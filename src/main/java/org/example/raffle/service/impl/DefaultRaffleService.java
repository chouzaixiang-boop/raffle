package org.example.raffle.service.impl;

import org.example.raffle.domain.RaffleContext;
import org.example.raffle.domain.RaffleRecord;
import org.example.raffle.domain.RaffleResult;
import org.example.raffle.domain.Strategy;
import org.example.raffle.domain.StrategyAward;
import org.example.raffle.repository.AwardRepository;
import org.example.raffle.repository.RaffleRecordRepository;
import org.example.raffle.repository.RuleRepository;
import org.example.raffle.repository.StrategyAwardRepository;
import org.example.raffle.repository.StrategyRepository;
import org.example.raffle.rule.RuleHandler;
import org.example.raffle.rule.RuleHandlerRegistry;
import org.example.raffle.service.RaffleService;
import org.example.raffle.service.RaffleStateService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class DefaultRaffleService implements RaffleService {

    private final StrategyRepository strategyRepository;
    private final StrategyAwardRepository strategyAwardRepository;
    private final AwardRepository awardRepository;
    private final RuleRepository ruleRepository;
    private final RaffleRecordRepository raffleRecordRepository;
    private final RuleHandlerRegistry ruleHandlerRegistry;
    private final RaffleStateService raffleStateService;
    private final Random random = new Random();

    public DefaultRaffleService(StrategyRepository strategyRepository,
                                StrategyAwardRepository strategyAwardRepository,
                                AwardRepository awardRepository,
                                RuleRepository ruleRepository,
                                RaffleRecordRepository raffleRecordRepository,
                                RuleHandlerRegistry ruleHandlerRegistry,
                                RaffleStateService raffleStateService) {
        this.strategyRepository = strategyRepository;
        this.strategyAwardRepository = strategyAwardRepository;
        this.awardRepository = awardRepository;
        this.ruleRepository = ruleRepository;
        this.raffleRecordRepository = raffleRecordRepository;
        this.ruleHandlerRegistry = ruleHandlerRegistry;
        this.raffleStateService = raffleStateService;
    }

    @Override
    public RaffleResult draw(Long userId, Long strategyId) {
        Strategy strategy = strategyRepository.findById(strategyId)
                .orElseThrow(() -> new IllegalArgumentException("strategy not found: " + strategyId));

        RaffleContext context = new RaffleContext(userId, strategyId);
        applyRules(strategy.ruleModels(), context);
        if (context.getActualStrategyId() == 0L) {
            finalizeAsConsolation(context);
            return persist(context);
        }

        StrategyAward selectedAward = selectAward(context.getActualStrategyId());
        context.setAwardId(selectedAward.awardId());
        context.setAwardName(awardRepository.findById(selectedAward.awardId())
                .map(award -> award.awardName())
                .orElse(selectedAward.awardTitle()));

        applyRules(strategyAwardRepository.findByStrategyIdAndAwardId(context.getActualStrategyId(), context.getAwardId()).ruleModels(), context);
        if (!context.isSuccess()) {
            finalizeAsConsolation(context);
        }

        if (context.isSuccess()) {
            raffleStateService.incrementUserCount(userId, strategyId);
        }
        return persist(context);
    }

    private void applyRules(List<String> ruleModels, RaffleContext context) {
        for (String ruleModel : ruleModels) {
            RuleHandler ruleHandler = ruleHandlerRegistry.getHandler(ruleModel);
            ruleHandler.apply(context);
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
}
