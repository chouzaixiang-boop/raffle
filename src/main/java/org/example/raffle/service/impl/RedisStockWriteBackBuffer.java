package org.example.raffle.service.impl;

import org.example.raffle.cache.RedisStaticDataCache;
import org.example.raffle.repository.mysql.MySqlStrategyAwardRepository;
import org.example.raffle.repository.mysql.po.StockDeltaRow;
import org.example.raffle.service.RaffleStateService;
import org.example.raffle.service.StockWriteBackBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Profile("!local")
public class RedisStockWriteBackBuffer implements StockWriteBackBuffer {

    private static final Logger log = LoggerFactory.getLogger(RedisStockWriteBackBuffer.class);

    private static final String PENDING_KEY = "lottery:stock:deduct:pending";
    private static final String PROCESSING_KEY_PREFIX = "lottery:stock:deduct:processing:";

    private final StringRedisTemplate redisTemplate;
    private final MySqlStrategyAwardRepository mySqlStrategyAwardRepository;
    private final RedisStaticDataCache cache;
    private final RaffleStateService raffleStateService;

    public RedisStockWriteBackBuffer(StringRedisTemplate redisTemplate,
                                     MySqlStrategyAwardRepository mySqlStrategyAwardRepository,
                                     RedisStaticDataCache cache,
                                     RaffleStateService raffleStateService) {
        this.redisTemplate = redisTemplate;
        this.mySqlStrategyAwardRepository = mySqlStrategyAwardRepository;
        this.cache = cache;
        this.raffleStateService = raffleStateService;
    }

    @Override
    public void enqueueDecrease(Long strategyId, Long awardId) {
        redisTemplate.opsForHash().increment(PENDING_KEY, stockField(strategyId, awardId), 1L);
    }

    @Scheduled(fixedDelayString = "${raffle.stock.writeback.interval-ms:5000}")
    public void flushPendingDecreases() {
        long startTime = System.currentTimeMillis();
        boolean hasQueue = Boolean.TRUE.equals(redisTemplate.hasKey(PENDING_KEY));
        
        if (!hasQueue) {
            if (log.isDebugEnabled()) {
                log.debug("raffle_stock_writeback_tick status=idle queue_exists=false");
            }
            return;
        }

        String processingKey = PROCESSING_KEY_PREFIX + System.currentTimeMillis();
        if (!Boolean.TRUE.equals(redisTemplate.renameIfAbsent(PENDING_KEY, processingKey))) {
            if (log.isDebugEnabled()) {
                log.debug("raffle_stock_writeback_tick status=skip reason=rename_failed");
            }
            return;
        }

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(processingKey);
        if (entries == null || entries.isEmpty()) {
            redisTemplate.delete(processingKey);
            if (log.isDebugEnabled()) {
                log.debug("raffle_stock_writeback_tick status=empty queue_size=0");
            }
            return;
        }

        List<StockDeltaRow> items = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            StockDeltaRow item = parseDelta(entry.getKey(), entry.getValue());
            if (item != null && item.getDecreaseCount() > 0) {
                items.add(item);
            }
        }

        if (items.isEmpty()) {
            redisTemplate.delete(processingKey);
            if (log.isDebugEnabled()) {
                log.debug("raffle_stock_writeback_tick status=no_valid_items raw_count={}", entries.size());
            }
            return;
        }

        try {
            long dbStartTime = System.currentTimeMillis();
            mySqlStrategyAwardRepository.batchDecreaseSurplus(items);
            long dbCostMs = System.currentTimeMillis() - dbStartTime;
            
            for (StockDeltaRow item : items) {
                int latestStock = raffleStateService.getStock(item.getStrategyId(), item.getAwardId());
                cache.updateStrategyAwardSurplus(item.getStrategyId(), item.getAwardId(), latestStock);
            }
            redisTemplate.delete(processingKey);
            
            long totalCostMs = System.currentTimeMillis() - startTime;
            log.info("raffle_stock_writeback_tick status=success items={} db_cost_ms={} total_cost_ms={}", 
                     items.size(), dbCostMs, totalCostMs);
        } catch (Exception ex) {
            for (StockDeltaRow item : items) {
                redisTemplate.opsForHash().increment(PENDING_KEY, stockField(item.getStrategyId(), item.getAwardId()), item.getDecreaseCount());
            }
            redisTemplate.delete(processingKey);
            long totalCostMs = System.currentTimeMillis() - startTime;
            log.error("raffle_stock_writeback_tick status=failed items={} cost_ms={} error={}", 
                      items.size(), totalCostMs, ex.getMessage(), ex);
        }
    }

    private String stockField(Long strategyId, Long awardId) {
        return strategyId + "_" + awardId;
    }

    private StockDeltaRow parseDelta(Object rawField, Object rawValue) {
        if (rawField == null || rawValue == null) {
            return null;
        }
        String field = rawField.toString();
        String[] parts = field.split("_", 2);
        if (parts.length != 2) {
            return null;
        }
        try {
            Long strategyId = Long.parseLong(parts[0]);
            Long awardId = Long.parseLong(parts[1]);
            long decreaseCount = Long.parseLong(rawValue.toString());
            if (decreaseCount <= 0) {
                return null;
            }
            return new StockDeltaRow(strategyId, awardId, decreaseCount);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}