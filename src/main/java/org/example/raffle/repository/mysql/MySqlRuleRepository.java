package org.example.raffle.repository.mysql;

import org.example.raffle.repository.RuleRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("!local")
public class MySqlRuleRepository implements RuleRepository {

    private final JdbcTemplate jdbcTemplate;

    public MySqlRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<String> findRulesByStrategyId(Long strategyId) {
        return jdbcTemplate.query(
                "select rule_model from strategy_rule where strategy_id = ? and award_id is null order by id",
                (resultSet, rowNum) -> resultSet.getString("rule_model"),
                strategyId
        );
    }

    @Override
    public String findRuleValue(Long strategyId, Long awardId, String ruleModel) {
        if (awardId == null) {
            return jdbcTemplate.query(
                    "select rule_value from strategy_rule where strategy_id = ? and award_id is null and rule_model = ? limit 1",
                    (resultSet, rowNum) -> resultSet.getString("rule_value"),
                    strategyId,
                    ruleModel
            ).stream().findFirst().orElse(null);
        }
        return jdbcTemplate.query(
                "select rule_value from strategy_rule where strategy_id = ? and award_id = ? and rule_model = ? limit 1",
                (resultSet, rowNum) -> resultSet.getString("rule_value"),
                strategyId,
                awardId,
                ruleModel
        ).stream().findFirst().orElse(null);
    }
}
