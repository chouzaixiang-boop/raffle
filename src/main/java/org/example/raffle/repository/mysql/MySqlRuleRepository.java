package org.example.raffle.repository.mysql;

import org.example.raffle.domain.RuleConfig;
import org.example.raffle.repository.RuleRepository;
import org.example.raffle.repository.mysql.mapper.RuleMapper;
import org.example.raffle.repository.mysql.po.RuleRow;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("!local")
public class MySqlRuleRepository implements RuleRepository {

    private final RuleMapper ruleMapper;

    public MySqlRuleRepository(RuleMapper ruleMapper) {
        this.ruleMapper = ruleMapper;
    }

    @Override
    public List<String> findRulesByStrategyId(Long strategyId) {
        return ruleMapper.findRuleModelsByStrategyId(strategyId);
    }

    @Override
    public String findRuleValue(Long strategyId, Long awardId, String ruleModel) {
        if (awardId == null) {
            return ruleMapper.findRuleValueForStrategy(strategyId, ruleModel);
        }
        return ruleMapper.findRuleValueForAward(strategyId, awardId, ruleModel);
    }

    @Override
    public List<RuleConfig> findAll() {
        return ruleMapper.findAll().stream().map(this::toDomain).toList();
    }

    private RuleConfig toDomain(RuleRow row) {
        return new RuleConfig(row.getStrategyId(), row.getAwardId(), row.getRuleModel(), row.getRuleValue(), row.getRuleDesc());
    }
}
