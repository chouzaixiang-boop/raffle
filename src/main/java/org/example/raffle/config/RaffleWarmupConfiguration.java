package org.example.raffle.config;

import org.example.raffle.repository.StrategyAwardRepository;
import org.example.raffle.service.impl.RedisRaffleStateService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!local")
public class RaffleWarmupConfiguration {

    @Bean
    CommandLineRunner warmupStockCache(StrategyAwardRepository strategyAwardRepository,
                                       RedisRaffleStateService raffleStateService) {
        return args -> strategyAwardRepository.findAll().forEach(strategyAward -> {
            if (strategyAward.awardSurplus() >= 0) {
                raffleStateService.initializeStock(strategyAward.strategyId(), strategyAward.awardId(), strategyAward.awardSurplus());
            }
        });
    }
}
