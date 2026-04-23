package org.example.raffle.rule.impl;

import org.example.raffle.domain.RaffleContext;
import org.example.raffle.repository.RuleRepository;
import org.example.raffle.rule.RuleHandler;
import org.example.raffle.service.RaffleStateService;
import org.springframework.stereotype.Component;

@Component
public class LockRuleHandler implements RuleHandler {

    private static final String RULE_MODEL = "rule_lock";
    private final RuleRepository ruleRepository;
    private final RaffleStateService raffleStateService;

    public LockRuleHandler(RuleRepository ruleRepository, RaffleStateService raffleStateService) {
        this.ruleRepository = ruleRepository;
        this.raffleStateService = raffleStateService;
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
        int requiredCount = Integer.parseInt(ruleValue.trim());
        long currentCount = raffleStateService.getUserCount(context.getUserId(), context.getStrategyId());
        if (currentCount + 1 < requiredCount) {
            context.setAwardId(raffleStateService.getConsolationAwardId());
            context.setAwardName(raffleStateService.getAwardName(context.getAwardId()));
            context.setSuccess(false);
            context.setMessage("user count does not meet lock rule");
        }
    }
}
