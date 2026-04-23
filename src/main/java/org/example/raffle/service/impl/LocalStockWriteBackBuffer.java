package org.example.raffle.service.impl;

import org.example.raffle.repository.StrategyAwardRepository;
import org.example.raffle.service.RaffleStateService;
import org.example.raffle.service.StockWriteBackBuffer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class LocalStockWriteBackBuffer implements StockWriteBackBuffer {

    private final StrategyAwardRepository strategyAwardRepository;
    private final RaffleStateService raffleStateService;

    public LocalStockWriteBackBuffer(StrategyAwardRepository strategyAwardRepository, RaffleStateService raffleStateService) {
        this.strategyAwardRepository = strategyAwardRepository;
        this.raffleStateService = raffleStateService;
    }

    @Override
    public void enqueueDecrease(Long strategyId, Long awardId) {
        int stock = raffleStateService.getStock(strategyId, awardId);
        strategyAwardRepository.updateSurplus(strategyId, awardId, stock);
    }
}