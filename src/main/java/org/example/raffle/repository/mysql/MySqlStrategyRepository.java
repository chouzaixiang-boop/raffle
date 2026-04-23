package org.example.raffle.repository.mysql;

import org.example.raffle.domain.Strategy;
import org.example.raffle.repository.StrategyRepository;
import org.example.raffle.repository.mysql.mapper.StrategyMapper;
import org.example.raffle.repository.mysql.po.StrategyRow;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!local")
public class MySqlStrategyRepository implements StrategyRepository {

    private final StrategyMapper strategyMapper;

    public MySqlStrategyRepository(StrategyMapper strategyMapper) {
        this.strategyMapper = strategyMapper;
    }

    @Override
    public Optional<Strategy> findById(Long strategyId) {
        return Optional.ofNullable(strategyMapper.findByStrategyId(strategyId)).map(this::toDomain);
    }

    @Override
    public List<Strategy> findAll() {
        return strategyMapper.findAll().stream().map(this::toDomain).toList();
    }

    private Strategy toDomain(StrategyRow row) {
        return new Strategy(row.getStrategyId(), row.getStrategyDesc(), splitCsv(row.getRuleModels()));
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
