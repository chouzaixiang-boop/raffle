package org.example.raffle.repository.mysql;

import org.example.raffle.domain.Strategy;
import org.example.raffle.repository.StrategyRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!local")
public class MySqlStrategyRepository implements StrategyRepository {

    private final JdbcTemplate jdbcTemplate;

    public MySqlStrategyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Strategy> findById(Long strategyId) {
        return jdbcTemplate.query(
                "select strategy_id, strategy_desc, rule_models from strategy where strategy_id = ?",
                (resultSet, rowNum) -> new Strategy(
                        resultSet.getLong("strategy_id"),
                        resultSet.getString("strategy_desc"),
                        splitCsv(resultSet.getString("rule_models"))
                ),
                strategyId
        ).stream().findFirst();
    }

    @Override
    public List<Strategy> findAll() {
        return jdbcTemplate.query(
                "select strategy_id, strategy_desc, rule_models from strategy order by id",
                (resultSet, rowNum) -> new Strategy(
                        resultSet.getLong("strategy_id"),
                        resultSet.getString("strategy_desc"),
                        splitCsv(resultSet.getString("rule_models"))
                )
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
