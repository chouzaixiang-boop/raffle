package org.example.raffle.repository.mysql;

import org.example.raffle.domain.StrategyAward;
import org.example.raffle.repository.StrategyAwardRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Repository
@Profile("!local")
public class MySqlStrategyAwardRepository implements StrategyAwardRepository {

    private final JdbcTemplate jdbcTemplate;

    public MySqlStrategyAwardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<StrategyAward> findByStrategyId(Long strategyId) {
        return jdbcTemplate.query(
                "select strategy_id, award_id, award_title, rule_models, award_allocate, award_surplus, award_rate, award_index from strategy_award where strategy_id = ? order by award_index",
                (resultSet, rowNum) -> mapRow(resultSet),
                strategyId
        );
    }

    @Override
    public List<StrategyAward> findAll() {
        return jdbcTemplate.query(
                "select strategy_id, award_id, award_title, rule_models, award_allocate, award_surplus, award_rate, award_index from strategy_award order by strategy_id, award_index",
                (resultSet, rowNum) -> mapRow(resultSet)
        );
    }

    @Override
    public StrategyAward findByStrategyIdAndAwardId(Long strategyId, Long awardId) {
        return jdbcTemplate.query(
                "select strategy_id, award_id, award_title, rule_models, award_allocate, award_surplus, award_rate, award_index from strategy_award where strategy_id = ? and award_id = ?",
                (resultSet, rowNum) -> mapRow(resultSet),
                strategyId,
                awardId
        ).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("award config not found: strategyId=" + strategyId + ", awardId=" + awardId));
    }

    private StrategyAward mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new StrategyAward(
                resultSet.getLong("strategy_id"),
                resultSet.getLong("award_id"),
                resultSet.getString("award_title"),
                splitCsv(resultSet.getString("rule_models")),
                resultSet.getInt("award_allocate"),
                resultSet.getInt("award_surplus"),
                resultSet.getBigDecimal("award_rate"),
                resultSet.getInt("award_index")
        );
    }

    @Override
    public void updateSurplus(Long strategyId, Long awardId, int surplus) {
        jdbcTemplate.update(
                "update strategy_award set award_surplus = ? where strategy_id = ? and award_id = ?",
                surplus,
                strategyId,
                awardId
        );
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
