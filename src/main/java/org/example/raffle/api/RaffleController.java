package org.example.raffle.api;

import org.example.raffle.domain.RaffleResult;
import org.example.raffle.service.RaffleService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/raffle")
public class RaffleController {

    private final RaffleService raffleService;

    public RaffleController(RaffleService raffleService) {
        this.raffleService = raffleService;
    }

    @PostMapping("/draw")
    public DrawResponse draw(@RequestBody DrawRequest request) {
        if (request == null || request.userId() == null || request.strategyId() == null) {
            throw new IllegalArgumentException("userId and strategyId are required");
        }
        RaffleResult result = raffleService.draw(request.userId(), request.strategyId());
        return new DrawResponse(result.userId(), result.strategyId(), result.awardId(), result.awardName(), result.success(), result.message());
    }
}
