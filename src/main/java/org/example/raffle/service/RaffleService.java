package org.example.raffle.service;

import org.example.raffle.domain.RaffleResult;

public interface RaffleService {

    RaffleResult draw(Long userId, Long strategyId);
}
