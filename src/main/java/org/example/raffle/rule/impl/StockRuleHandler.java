package org.example.raffle.rule.impl;

import org.example.raffle.domain.RaffleContext;
import org.example.raffle.repository.RuleRepository;
import org.example.raffle.rule.RuleHandler;
import org.example.raffle.service.RaffleStateService;
import org.example.raffle.service.StockWriteBackBuffer;
import org.springframework.stereotype.Component;

@Component
public class StockRuleHandler implements RuleHandler {

    private static final String RULE_MODEL = "rule_stock";
    private final RuleRepository ruleRepository;
    private final RaffleStateService raffleStateService;
    private final StockWriteBackBuffer stockWriteBackBuffer;

    public StockRuleHandler(RuleRepository ruleRepository,
                            RaffleStateService raffleStateService,
                            StockWriteBackBuffer stockWriteBackBuffer) {
        this.ruleRepository = ruleRepository;
        this.raffleStateService = raffleStateService;
        this.stockWriteBackBuffer = stockWriteBackBuffer;
    }

    @Override
    public boolean supports(String ruleModel) {
        return RULE_MODEL.equals(ruleModel);
    }

    @Override
    public void apply(RaffleContext context) {
        String ruleValue = ruleRepository.findRuleValue(context.getActualStrategyId(), context.getAwardId(), RULE_MODEL);
        if (ruleValue == null || ruleValue.isBlank()) {
            return;
        }
        int stock = raffleStateService.decreaseStock(context.getActualStrategyId(), context.getAwardId());
        if (stock < 0) {
            context.setAwardId(raffleStateService.getConsolationAwardId());
            context.setAwardName(raffleStateService.getAwardName(context.getAwardId()));
            context.setSuccess(false);
            context.setMessage("stock is exhausted");
        } else {
            stockWriteBackBuffer.enqueueDecrease(context.getActualStrategyId(), context.getAwardId());
        }
    }
}
