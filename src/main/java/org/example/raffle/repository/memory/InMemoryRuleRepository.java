package org.example.raffle.repository.memory;

import org.example.raffle.domain.RuleConfig;
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

    @Override
    public List<RuleConfig> findAll() {
        List<RuleConfig> configs = new ArrayList<>();
        for (Map.Entry<Long, List<String>> entry : strategyRules.entrySet()) {
            Long strategyId = entry.getKey();
            for (String ruleModel : entry.getValue()) {
                String value = ruleValues.get(key(strategyId, null, ruleModel));
                configs.add(new RuleConfig(strategyId, null, ruleModel, value, null));
            }
        }
        for (Map.Entry<String, String> entry : ruleValues.entrySet()) {
            String[] parts = entry.getKey().split(":", -1);
            if (parts.length != 3) {
                continue;
            }
            Long strategyId = Long.valueOf(parts[0]);
            Long awardId = "null".equals(parts[1]) ? null : Long.valueOf(parts[1]);
            String ruleModel = parts[2];
            String ruleValue = entry.getValue();
            boolean exists = configs.stream().anyMatch(item -> equalsConfig(item, strategyId, awardId, ruleModel));
            if (!exists) {
                configs.add(new RuleConfig(strategyId, awardId, ruleModel, ruleValue, null));
            }
        }
        return configs;
    }

    private boolean equalsConfig(RuleConfig config, Long strategyId, Long awardId, String ruleModel) {
        return config.strategyId().equals(strategyId)
                && (config.awardId() == null ? awardId == null : config.awardId().equals(awardId))
                && config.ruleModel().equals(ruleModel);
    }

    private String key(Long strategyId, Long awardId, String ruleModel) {
        return strategyId + ":" + awardId + ":" + ruleModel;
    }
}
