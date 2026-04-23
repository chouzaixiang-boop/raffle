package org.example.raffle.config;

import org.example.raffle.cache.RedisStaticDataCache;
import org.example.raffle.domain.Award;
import org.example.raffle.domain.RuleConfig;
import org.example.raffle.domain.Strategy;
import org.example.raffle.domain.StrategyAward;
import org.example.raffle.repository.mysql.MySqlAwardRepository;
import org.example.raffle.repository.mysql.MySqlRuleRepository;
import org.example.raffle.repository.mysql.MySqlStrategyAwardRepository;
import org.example.raffle.repository.mysql.MySqlStrategyRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
public class StaticDataCacheWarmupService {

    private final MySqlStrategyRepository strategyRepository;
    private final MySqlAwardRepository awardRepository;
    private final MySqlStrategyAwardRepository strategyAwardRepository;
    private final MySqlRuleRepository ruleRepository;
    private final RedisStaticDataCache cache;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    public StaticDataCacheWarmupService(MySqlStrategyRepository strategyRepository,
                                        MySqlAwardRepository awardRepository,
                                        MySqlStrategyAwardRepository strategyAwardRepository,
                                        MySqlRuleRepository ruleRepository,
                                        RedisStaticDataCache cache) {
        this.strategyRepository = strategyRepository;
        this.awardRepository = awardRepository;
        this.strategyAwardRepository = strategyAwardRepository;
        this.ruleRepository = ruleRepository;
        this.cache = cache;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmupOnStartup() {
        refreshAll();
    }

    @Scheduled(fixedDelayString = "${raffle.cache.refresh-interval-ms:300000}")
    public void scheduledRefresh() {
        refreshAll();
    }

    public void refreshAll() {
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }
        try {
            List<Strategy> strategies = strategyRepository.findAll();
            cache.putAllStrategies(strategies);

            Map<Long, Award> awards = awardRepository.findAll();
            cache.putAllAwards(awards);

            List<StrategyAward> strategyAwards = strategyAwardRepository.findAll();
            strategyAwards.stream()
                    .collect(Collectors.groupingBy(StrategyAward::strategyId))
                    .forEach(cache::putStrategyAwards);
            strategyAwards.forEach(cache::putStrategyAward);

            List<RuleConfig> rules = ruleRepository.findAll();
            rules.stream()
                    .collect(Collectors.groupingBy(RuleConfig::strategyId))
                    .forEach((strategyId, ruleConfigs) -> cache.putRuleConfigs(strategyId, ruleConfigs));
        } finally {
            refreshing.set(false);
        }
    }
}