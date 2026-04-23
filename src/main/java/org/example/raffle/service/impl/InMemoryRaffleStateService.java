package org.example.raffle.service.impl;

import org.example.raffle.repository.AwardRepository;
import org.example.raffle.service.RaffleStateService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Profile("local")
public class InMemoryRaffleStateService implements RaffleStateService {

    private static final Long CONSOLATION_AWARD_ID = 101L;
    private final Map<String, AtomicInteger> stockMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> userCountMap = new ConcurrentHashMap<>();
    private final AwardRepository awardRepository;

    public InMemoryRaffleStateService(AwardRepository awardRepository) {
        this.awardRepository = awardRepository;
    }

    public void putStock(Long strategyId, Long awardId, int stock) {
        stockMap.put(key(strategyId, awardId), new AtomicInteger(stock));
    }

    @Override
    public void incrementUserCount(Long userId, Long strategyId) {
        userCountMap.computeIfAbsent(userCountKey(userId, strategyId), key -> new AtomicLong()).incrementAndGet();
    }

    @Override
    public long getUserCount(Long userId, Long strategyId) {
        return userCountMap.getOrDefault(userCountKey(userId, strategyId), new AtomicLong()).get();
    }

    @Override
    public int decreaseStock(Long strategyId, Long awardId) {
        AtomicInteger stock = stockMap.get(key(strategyId, awardId));
        if (stock == null) {
            return -1;
        }
        int current = stock.decrementAndGet();
        if (current < 0) {
            stock.incrementAndGet();
            return -1;
        }
        return current;
    }

    @Override
    public int getStock(Long strategyId, Long awardId) {
        AtomicInteger stock = stockMap.get(key(strategyId, awardId));
        return stock == null ? 0 : stock.get();
    }

    @Override
    public Long getConsolationAwardId() {
        return CONSOLATION_AWARD_ID;
    }

    @Override
    public String getAwardName(Long awardId) {
        return awardRepository.findById(awardId)
                .map(award -> award.awardName())
                .orElse("谢谢惠顾");
    }

    private String key(Long strategyId, Long awardId) {
        return strategyId + ":" + awardId;
    }

    private String userCountKey(Long userId, Long strategyId) {
        return userId + ":" + strategyId;
    }
}
