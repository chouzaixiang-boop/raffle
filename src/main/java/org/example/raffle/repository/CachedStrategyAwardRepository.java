package org.example.raffle.repository;

import org.example.raffle.cache.RedisStaticDataCache;
import org.example.raffle.domain.StrategyAward;
import org.example.raffle.repository.mysql.MySqlStrategyAwardRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
@Profile("!local")
public class CachedStrategyAwardRepository implements StrategyAwardRepository {

    private final MySqlStrategyAwardRepository mySqlStrategyAwardRepository;
    private final RedisStaticDataCache cache;

    public CachedStrategyAwardRepository(MySqlStrategyAwardRepository mySqlStrategyAwardRepository, RedisStaticDataCache cache) {
        this.mySqlStrategyAwardRepository = mySqlStrategyAwardRepository;
        this.cache = cache;
    }

    @Override
    public List<StrategyAward> findByStrategyId(Long strategyId) {
        return cache.getStrategyAwards(strategyId).orElseGet(() -> {
            List<StrategyAward> awards = mySqlStrategyAwardRepository.findByStrategyId(strategyId);
            cache.putStrategyAwards(strategyId, awards);
            awards.forEach(cache::putStrategyAward);
            return awards;
        });
    }

    @Override
    public List<StrategyAward> findAll() {
        return mySqlStrategyAwardRepository.findAll();
    }

    @Override
    public StrategyAward findByStrategyIdAndAwardId(Long strategyId, Long awardId) {
        StrategyAward cached = cache.getStrategyAward(strategyId, awardId).orElse(null);
        if (cached != null) {
            return cached;
        }
        StrategyAward award = mySqlStrategyAwardRepository.findByStrategyIdAndAwardId(strategyId, awardId);
        cache.putStrategyAward(award);
        return award;
    }

    @Override
    public void updateSurplus(Long strategyId, Long awardId, int surplus) {
        mySqlStrategyAwardRepository.updateSurplus(strategyId, awardId, surplus);
        cache.updateStrategyAwardSurplus(strategyId, awardId, surplus);
    }
}