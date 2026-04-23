package org.example.raffle.repository;

import org.example.raffle.domain.Strategy;

import java.util.Optional;

public interface StrategyRepository {

    Optional<Strategy> findById(Long strategyId);

    java.util.List<Strategy> findAll();
}
