package org.example.raffle.service.impl;

import org.example.raffle.repository.AwardRepository;
import org.example.raffle.service.RaffleStateService;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!local")
public class RedisRaffleStateService implements RaffleStateService {

    private static final Long CONSOLATION_AWARD_ID = 101L;
    private static final String STOCK_PREFIX = "lottery:award_stock:";
    private static final String USER_COUNT_PREFIX = "lottery:user_count:";

    private final StringRedisTemplate redisTemplate;
    private final AwardRepository awardRepository;

    public RedisRaffleStateService(StringRedisTemplate redisTemplate, AwardRepository awardRepository) {
        this.redisTemplate = redisTemplate;
        this.awardRepository = awardRepository;
    }

    @Override
    public void incrementUserCount(Long userId, Long strategyId) {
        redisTemplate.opsForValue().increment(userCountKey(userId, strategyId));
    }

    @Override
    public long getUserCount(Long userId, Long strategyId) {
        String value = redisTemplate.opsForValue().get(userCountKey(userId, strategyId));
        return value == null ? 0L : Long.parseLong(value);
    }

    @Override
    public int decreaseStock(Long strategyId, Long awardId) {
        Long remaining = redisTemplate.opsForValue().decrement(stockKey(strategyId, awardId));
        if (remaining == null) {
            return -1;
        }
        if (remaining < 0) {
            redisTemplate.opsForValue().increment(stockKey(strategyId, awardId));
            return -1;
        }
        return remaining.intValue();
    }

    @Override
    public int increaseStock(Long strategyId, Long awardId, int maxStock) {
        if (maxStock < 0) {
            throw new IllegalArgumentException("maxStock must be >= 0");
        }
        Long remaining = redisTemplate.opsForValue().increment(stockKey(strategyId, awardId));
        if (remaining == null) {
            return -1;
        }
        if (remaining > maxStock) {
            redisTemplate.opsForValue().decrement(stockKey(strategyId, awardId));
            return maxStock;
        }
        return remaining.intValue();
    }

    @Override
    public int getStock(Long strategyId, Long awardId) {
        String value = redisTemplate.opsForValue().get(stockKey(strategyId, awardId));
        return value == null ? 0 : Integer.parseInt(value);
    }

    @Override
    public void setStock(Long strategyId, Long awardId, int stock) {
        redisTemplate.opsForValue().set(stockKey(strategyId, awardId), String.valueOf(stock));
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

    public void initializeStock(Long strategyId, Long awardId, int stock) {
        setStock(strategyId, awardId, stock);
    }

    private String stockKey(Long strategyId, Long awardId) {
        return STOCK_PREFIX + strategyId + '_' + awardId;
    }

    private String userCountKey(Long userId, Long strategyId) {
        return USER_COUNT_PREFIX + userId + '_' + strategyId;
    }
}
