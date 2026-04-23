package org.example.raffle.repository.memory;

import org.example.raffle.domain.StrategyAward;
import org.example.raffle.repository.StrategyAwardRepository;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@Profile("local")
public class InMemoryStrategyAwardRepository implements StrategyAwardRepository {

    private final Map<Long, List<StrategyAward>> awardsByStrategy = new LinkedHashMap<>();

    public void put(StrategyAward strategyAward) {
        awardsByStrategy.computeIfAbsent(strategyAward.strategyId(), key -> new ArrayList<>()).add(strategyAward);
        awardsByStrategy.get(strategyAward.strategyId()).sort(Comparator.comparingInt(StrategyAward::awardIndex));
    }

    @Override
    public List<StrategyAward> findByStrategyId(Long strategyId) {
        return List.copyOf(awardsByStrategy.getOrDefault(strategyId, List.of()));
    }

    @Override
    public List<StrategyAward> findAll() {
        return awardsByStrategy.values().stream().flatMap(List::stream).toList();
    }

    @Override
    public StrategyAward findByStrategyIdAndAwardId(Long strategyId, Long awardId) {
        return awardsByStrategy.getOrDefault(strategyId, List.of())
                .stream()
                .filter(item -> item.awardId().equals(awardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("award config not found: strategyId=" + strategyId + ", awardId=" + awardId));
    }

    @Override
    public void updateSurplus(Long strategyId, Long awardId, int surplus) {
        List<StrategyAward> awards = awardsByStrategy.get(strategyId);
        if (awards == null) {
            return;
        }
        for (int index = 0; index < awards.size(); index++) {
            StrategyAward item = awards.get(index);
            if (item.awardId().equals(awardId)) {
                awards.set(index, new StrategyAward(
                        item.strategyId(),
                        item.awardId(),
                        item.awardTitle(),
                        item.ruleModels(),
                        item.awardAllocate(),
                        surplus,
                        item.awardRate(),
                        item.awardIndex()
                ));
                break;
            }
        }
    }
}
