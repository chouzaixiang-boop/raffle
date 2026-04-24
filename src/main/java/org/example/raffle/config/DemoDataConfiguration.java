package org.example.raffle.config;

import org.example.raffle.domain.Activity;
import org.example.raffle.domain.Award;
import org.example.raffle.domain.Strategy;
import org.example.raffle.domain.StrategyAward;
import org.example.raffle.repository.memory.InMemoryActivityRepository;
import org.example.raffle.repository.memory.InMemoryAwardRepository;
import org.example.raffle.repository.memory.InMemoryRuleRepository;
import org.example.raffle.repository.memory.InMemoryStrategyAwardRepository;
import org.example.raffle.repository.memory.InMemoryStrategyRepository;
import org.example.raffle.service.impl.InMemoryRaffleStateService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@Profile("local")
public class DemoDataConfiguration {

    @Bean
    CommandLineRunner initDemoData(InMemoryAwardRepository awardRepository,
                                   InMemoryActivityRepository activityRepository,
                                   InMemoryStrategyRepository strategyRepository,
                                   InMemoryStrategyAwardRepository strategyAwardRepository,
                                   InMemoryRuleRepository ruleRepository,
                                   InMemoryRaffleStateService raffleStateService) {
        return args -> {
            awardRepository.put(new Award(101L, "谢谢惠顾", "virtual", "0", "兜底奖品"));
            awardRepository.put(new Award(201L, "五元优惠券", "virtual", "5", "小额优惠券"));
            awardRepository.put(new Award(202L, "充电宝", "physical", "1", "实物奖品"));
            awardRepository.put(new Award(203L, "笔记本电脑", "physical", "1", "大奖"));

            activityRepository.put(new Activity(20001L, "618 大促", "618 主题抽奖活动", 1001L, "618 抽奖", "天天抽大奖", "/images/activity-618.png", "#ff6a00", 1, 1));
            activityRepository.put(new Activity(20002L, "周年庆", "周年庆主题抽奖活动", 1002L, "周年庆抽奖", "参与就有机会", "/images/activity-anniversary.png", "#1f8ef1", 2, 1));

            strategyRepository.put(new Strategy(1001L, "默认抽奖策略", List.of("rule_blacklist", "rule_weight")));
            strategyRepository.put(new Strategy(1002L, "加权抽奖策略", List.of()));

            strategyAwardRepository.put(new StrategyAward(1001L, 101L, "谢谢惠顾", List.of(), 0, 0, new BigDecimal("0.00"), 0));
            strategyAwardRepository.put(new StrategyAward(1001L, 201L, "五元优惠券", List.of("rule_stock"), 100, 10, new BigDecimal("0.55"), 1));
            strategyAwardRepository.put(new StrategyAward(1001L, 202L, "充电宝", List.of("rule_stock"), 20, 2, new BigDecimal("0.35"), 2));
            strategyAwardRepository.put(new StrategyAward(1001L, 203L, "笔记本电脑", List.of("rule_lock", "rule_stock"), 5, 1, new BigDecimal("0.10"), 3));

            strategyAwardRepository.put(new StrategyAward(1002L, 101L, "谢谢惠顾", List.of(), 0, 0, new BigDecimal("0.00"), 0));
            strategyAwardRepository.put(new StrategyAward(1002L, 201L, "五元优惠券", List.of("rule_stock"), 100, 10, new BigDecimal("0.30"), 1));
            strategyAwardRepository.put(new StrategyAward(1002L, 202L, "充电宝", List.of("rule_stock"), 20, 2, new BigDecimal("0.40"), 2));
            strategyAwardRepository.put(new StrategyAward(1002L, 203L, "笔记本电脑", List.of("rule_lock", "rule_stock"), 5, 1, new BigDecimal("0.30"), 3));

            ruleRepository.putRuleValue(1001L, null, "rule_blacklist", "9001,9002");
            ruleRepository.putRuleValue(1001L, null, "rule_weight", "1003,1004;1002");
            ruleRepository.putRuleValue(1001L, 201L, "rule_stock", "true");
            ruleRepository.putRuleValue(1001L, 202L, "rule_stock", "true");
            ruleRepository.putRuleValue(1001L, 203L, "rule_lock", "3");
            ruleRepository.putRuleValue(1001L, 203L, "rule_stock", "true");
            ruleRepository.putRuleValue(1002L, 201L, "rule_stock", "true");
            ruleRepository.putRuleValue(1002L, 202L, "rule_stock", "true");
            ruleRepository.putRuleValue(1002L, 203L, "rule_lock", "1");
            ruleRepository.putRuleValue(1002L, 203L, "rule_stock", "true");

            raffleStateService.putStock(1001L, 201L, 10);
            raffleStateService.putStock(1001L, 202L, 2);
            raffleStateService.putStock(1001L, 203L, 1);
            raffleStateService.putStock(1002L, 201L, 10);
            raffleStateService.putStock(1002L, 202L, 2);
            raffleStateService.putStock(1002L, 203L, 1);
        };
    }
}
