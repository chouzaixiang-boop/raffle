package org.example.raffle.repository;

import java.util.List;

public interface RuleRepository {

    List<String> findRulesByStrategyId(Long strategyId);

    String findRuleValue(Long strategyId, Long awardId, String ruleModel);
}
