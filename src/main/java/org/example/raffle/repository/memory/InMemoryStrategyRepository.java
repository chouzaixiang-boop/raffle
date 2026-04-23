package org.example.raffle.repository.memory;

import org.example.raffle.domain.Strategy;
import org.example.raffle.repository.StrategyRepository;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@Profile("local")
public class InMemoryStrategyRepository implements StrategyRepository {

    private final Map<Long, Strategy> strategies = new LinkedHashMap<>();

    public void put(Strategy strategy) {
        strategies.put(strategy.strategyId(), strategy);
    }

    @Override
    public Optional<Strategy> findById(Long strategyId) {
        return Optional.ofNullable(strategies.get(strategyId));
    }

    @Override
    public List<Strategy> findAll() {
        return new ArrayList<>(strategies.values());
    }
}
