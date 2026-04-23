package org.example.raffle.repository;

import org.example.raffle.cache.RedisStaticDataCache;
import org.example.raffle.domain.RuleConfig;
import org.example.raffle.repository.mysql.MySqlRuleRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
@Profile("!local")
public class CachedRuleRepository implements RuleRepository {

    private final MySqlRuleRepository mySqlRuleRepository;
    private final RedisStaticDataCache cache;

    public CachedRuleRepository(MySqlRuleRepository mySqlRuleRepository, RedisStaticDataCache cache) {
        this.mySqlRuleRepository = mySqlRuleRepository;
        this.cache = cache;
    }

    @Override
    public List<String> findRulesByStrategyId(Long strategyId) {
        return cache.getRuleModels(strategyId)
                .orElseGet(() -> {
                    List<String> rules = mySqlRuleRepository.findRulesByStrategyId(strategyId);
                    cache.putRuleModels(strategyId, rules);
                    return rules;
                });
    }

    @Override
    public String findRuleValue(Long strategyId, Long awardId, String ruleModel) {
        return cache.getRuleValue(strategyId, awardId, ruleModel)
                .orElseGet(() -> {
                    String value = mySqlRuleRepository.findRuleValue(strategyId, awardId, ruleModel);
                    if (value != null) {
                        cache.putRuleValue(strategyId, awardId, ruleModel, value);
                    }
                    return value;
                });
    }

    @Override
    public List<RuleConfig> findAll() {
        return mySqlRuleRepository.findAll();
    }
}