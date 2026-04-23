package org.example.raffle.service;

public interface RaffleStateService {

    long getUserCount(Long userId, Long strategyId);

    void incrementUserCount(Long userId, Long strategyId);

    int decreaseStock(Long strategyId, Long awardId);

    int getStock(Long strategyId, Long awardId);

    void setStock(Long strategyId, Long awardId, int stock);

    Long getConsolationAwardId();

    String getAwardName(Long awardId);
}
