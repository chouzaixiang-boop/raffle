package org.example.raffle.repository;

import org.example.raffle.cache.RedisStaticDataCache;
import org.example.raffle.domain.Award;
import org.example.raffle.repository.mysql.MySqlAwardRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
@Primary
@Profile("!local")
public class CachedAwardRepository implements AwardRepository {

    private final MySqlAwardRepository mySqlAwardRepository;
    private final RedisStaticDataCache cache;

    public CachedAwardRepository(MySqlAwardRepository mySqlAwardRepository, RedisStaticDataCache cache) {
        this.mySqlAwardRepository = mySqlAwardRepository;
        this.cache = cache;
    }

    @Override
    public Optional<Award> findById(Long awardId) {
        return cache.getAward(awardId)
                .or(() -> mySqlAwardRepository.findById(awardId).map(award -> {
                    cache.putAward(award);
                    return award;
                }));
    }

    @Override
    public Map<Long, Award> findAll() {
        return cache.getAllAwards().orElseGet(() -> {
            Map<Long, Award> awards = mySqlAwardRepository.findAll();
            cache.putAllAwards(awards);
            return awards;
        });
    }
}