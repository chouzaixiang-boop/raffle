package org.example.raffle.repository.memory;

import org.example.raffle.repository.RuleRepository;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@Profile("local")
public class InMemoryRuleRepository implements RuleRepository {

    private final Map<Long, List<String>> strategyRules = new LinkedHashMap<>();
    private final Map<String, String> ruleValues = new LinkedHashMap<>();

    public void putStrategyRule(Long strategyId, String ruleModel) {
        strategyRules.computeIfAbsent(strategyId, key -> new ArrayList<>()).add(ruleModel);
    }

    public void putRuleValue(Long strategyId, Long awardId, String ruleModel, String ruleValue) {
        ruleValues.put(key(strategyId, awardId, ruleModel), ruleValue);
    }

    @Override
    public List<String> findRulesByStrategyId(Long strategyId) {
        return List.copyOf(strategyRules.getOrDefault(strategyId, List.of()));
    }

    @Override
    public String findRuleValue(Long strategyId, Long awardId, String ruleModel) {
        return ruleValues.get(key(strategyId, awardId, ruleModel));
    }

    private String key(Long strategyId, Long awardId, String ruleModel) {
        return strategyId + ":" + awardId + ":" + ruleModel;
    }
}
