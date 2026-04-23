package org.example.raffle.repository;

import org.example.raffle.cache.RedisStaticDataCache;
import org.example.raffle.domain.Strategy;
import org.example.raffle.repository.mysql.MySqlStrategyRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
@Profile("!local")
public class CachedStrategyRepository implements StrategyRepository {

    private final MySqlStrategyRepository mySqlStrategyRepository;
    private final RedisStaticDataCache cache;

    public CachedStrategyRepository(MySqlStrategyRepository mySqlStrategyRepository, RedisStaticDataCache cache) {
        this.mySqlStrategyRepository = mySqlStrategyRepository;
        this.cache = cache;
    }

    @Override
    public Optional<Strategy> findById(Long strategyId) {
        return cache.getStrategy(strategyId)
                .or(() -> mySqlStrategyRepository.findById(strategyId).map(strategy -> {
                    cache.putStrategy(strategy);
                    return strategy;
                }));
    }

    @Override
    public List<Strategy> findAll() {
        return cache.getAllStrategies().orElseGet(() -> {
            List<Strategy> strategies = mySqlStrategyRepository.findAll();
            cache.putAllStrategies(strategies);
            return strategies;
        });
    }
}