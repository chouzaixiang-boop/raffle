package org.example.raffle.repository;

import org.example.raffle.domain.StrategyAward;

import java.util.List;

public interface StrategyAwardRepository {

    List<StrategyAward> findByStrategyId(Long strategyId);

    List<StrategyAward> findAll();

    StrategyAward findByStrategyIdAndAwardId(Long strategyId, Long awardId);

    void updateSurplus(Long strategyId, Long awardId, int surplus);

    int increaseSurplusWithCap(Long strategyId, Long awardId, int increaseCount);
}
