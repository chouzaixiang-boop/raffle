package org.example.raffle.repository;

import org.example.raffle.domain.RuleConfig;

import java.util.List;

public interface RuleRepository {

    List<String> findRulesByStrategyId(Long strategyId);

    String findRuleValue(Long strategyId, Long awardId, String ruleModel);

    List<RuleConfig> findAll();
}
