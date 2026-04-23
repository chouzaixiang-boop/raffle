package org.example.raffle.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.raffle.domain.Award;
import org.example.raffle.domain.RuleConfig;
import org.example.raffle.domain.Strategy;
import org.example.raffle.domain.StrategyAward;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class RedisStaticDataCache {

    private static final String STRATEGY_KEY_PREFIX = "lottery:cache:strategy:item:";
    private static final String STRATEGY_ALL_KEY = "lottery:cache:strategy:all";
    private static final String AWARD_KEY_PREFIX = "lottery:cache:award:item:";
    private static final String AWARD_ALL_KEY = "lottery:cache:award:all";
    private static final String STRATEGY_AWARD_KEY_PREFIX = "lottery:cache:strategy_award:strategy:";
    private static final String RULE_MODELS_KEY_PREFIX = "lottery:cache:rule:models:strategy:";
    private static final String RULE_VALUE_KEY_PREFIX = "lottery:cache:rule:value:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisStaticDataCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<Strategy> getStrategy(Long strategyId) {
        return readObject(strategyKey(strategyId), Strategy.class);
    }

    public void putStrategy(Strategy strategy) {
        writeObject(strategyKey(strategy.strategyId()), strategy);
    }

    public Optional<List<Strategy>> getAllStrategies() {
        return readList(STRATEGY_ALL_KEY, Strategy.class);
    }

    public void putAllStrategies(List<Strategy> strategies) {
        writeList(STRATEGY_ALL_KEY, strategies);
        strategies.forEach(this::putStrategy);
    }

    public Optional<Award> getAward(Long awardId) {
        return readObject(awardKey(awardId), Award.class);
    }

    public void putAward(Award award) {
        writeObject(awardKey(award.awardId()), award);
    }

    public Optional<Map<Long, Award>> getAllAwards() {
        return readMap(AWARD_ALL_KEY, Long.class, Award.class);
    }

    public void putAllAwards(Map<Long, Award> awards) {
        writeMap(AWARD_ALL_KEY, awards);
        awards.values().forEach(this::putAward);
    }

    public Optional<List<StrategyAward>> getStrategyAwards(Long strategyId) {
        return readList(strategyAwardsKey(strategyId), StrategyAward.class);
    }

    public void putStrategyAwards(Long strategyId, List<StrategyAward> awards) {
        writeList(strategyAwardsKey(strategyId), awards);
    }

    public void updateStrategyAwardSurplus(Long strategyId, Long awardId, int surplus) {
        List<StrategyAward> awards = getStrategyAwards(strategyId).orElseGet(ArrayList::new);
        List<StrategyAward> updated = new ArrayList<>(awards.size());
        boolean changed = false;
        for (StrategyAward award : awards) {
            if (award.awardId().equals(awardId)) {
                updated.add(new StrategyAward(
                        award.strategyId(),
                        award.awardId(),
                        award.awardTitle(),
                        award.ruleModels(),
                        award.awardAllocate(),
                        surplus,
                        award.awardRate(),
                        award.awardIndex()
                ));
                changed = true;
            } else {
                updated.add(award);
            }
        }
        if (changed) {
            putStrategyAwards(strategyId, updated);
            updated.stream()
                    .filter(item -> item.awardId().equals(awardId))
                    .findFirst()
                    .ifPresent(item -> writeObject(strategyAwardItemKey(strategyId, awardId), item));
        }
    }

    public Optional<StrategyAward> getStrategyAward(Long strategyId, Long awardId) {
        return readObject(strategyAwardItemKey(strategyId, awardId), StrategyAward.class);
    }

    public void putStrategyAward(StrategyAward strategyAward) {
        writeObject(strategyAwardItemKey(strategyAward.strategyId(), strategyAward.awardId()), strategyAward);
    }

    public Optional<List<String>> getRuleModels(Long strategyId) {
        return readList(ruleModelsKey(strategyId), String.class);
    }

    public void putRuleModels(Long strategyId, List<String> ruleModels) {
        writeList(ruleModelsKey(strategyId), ruleModels);
    }

    public Optional<String> getRuleValue(Long strategyId, Long awardId, String ruleModel) {
        return readObject(ruleValueKey(strategyId, awardId, ruleModel), String.class);
    }

    public void putRuleValue(Long strategyId, Long awardId, String ruleModel, String ruleValue) {
        if (ruleValue != null) {
            writeObject(ruleValueKey(strategyId, awardId, ruleModel), ruleValue);
        }
    }

    public void putRuleConfigs(Long strategyId, List<RuleConfig> ruleConfigs) {
        List<String> ruleModels = new ArrayList<>();
        for (RuleConfig ruleConfig : ruleConfigs) {
            if (ruleConfig.awardId() == null && !ruleModels.contains(ruleConfig.ruleModel())) {
                ruleModels.add(ruleConfig.ruleModel());
            }
            putRuleValue(ruleConfig.strategyId(), ruleConfig.awardId(), ruleConfig.ruleModel(), ruleConfig.ruleValue());
        }
        putRuleModels(strategyId, ruleModels);
    }

    private String strategyKey(Long strategyId) {
        return STRATEGY_KEY_PREFIX + strategyId;
    }

    private String awardKey(Long awardId) {
        return AWARD_KEY_PREFIX + awardId;
    }

    private String strategyAwardsKey(Long strategyId) {
        return STRATEGY_AWARD_KEY_PREFIX + strategyId;
    }

    private String strategyAwardItemKey(Long strategyId, Long awardId) {
        return STRATEGY_AWARD_KEY_PREFIX + strategyId + ":item:" + awardId;
    }

    private String ruleModelsKey(Long strategyId) {
        return RULE_MODELS_KEY_PREFIX + strategyId;
    }

    private String ruleValueKey(Long strategyId, Long awardId, String ruleModel) {
        return RULE_VALUE_KEY_PREFIX + strategyId + ':' + (awardId == null ? "GLOBAL" : awardId) + ':' + ruleModel;
    }

    private <T> Optional<T> readObject(String key, Class<T> type) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(objectMapper.readValue(value, type));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read cache key: " + key, ex);
        }
    }

    private <T> void writeObject(String key, T value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to write cache key: " + key, ex);
        }
    }

    private <T> Optional<List<T>> readList(String key, Class<T> elementType) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return Optional.ofNullable(objectMapper.readValue(value, listType));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read cache list key: " + key, ex);
        }
    }

    private void writeList(String key, List<?> value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to write cache list key: " + key, ex);
        }
    }

    private <K, V> Optional<Map<K, V>> readMap(String key, Class<K> keyType, Class<V> valueType) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            JavaType mapType = objectMapper.getTypeFactory().constructMapType(Map.class, keyType, valueType);
            return Optional.ofNullable(objectMapper.readValue(value, mapType));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read cache map key: " + key, ex);
        }
    }

    private void writeMap(String key, Map<?, ?> value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to write cache map key: " + key, ex);
        }
    }
}