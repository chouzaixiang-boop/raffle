package org.example.raffle.rule.impl;

import org.example.raffle.domain.RaffleContext;
import org.example.raffle.repository.RuleRepository;
import org.example.raffle.rule.RuleHandler;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BlacklistRuleHandler implements RuleHandler {

    private static final String RULE_MODEL = "rule_blacklist";
    private final RuleRepository ruleRepository;

    public BlacklistRuleHandler(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    public boolean supports(String ruleModel) {
        return RULE_MODEL.equals(ruleModel);
    }

    @Override
    public void apply(RaffleContext context) {
        String ruleValue = ruleRepository.findRuleValue(context.getStrategyId(), null, RULE_MODEL);
        if (ruleValue == null || ruleValue.isBlank()) {
            return;
        }
        Set<Long> blacklist = Arrays.stream(ruleValue.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
        if (blacklist.contains(context.getUserId())) {
            context.setActualStrategyId(0L);
            context.setSuccess(false);
            context.setMessage("user is in blacklist, fallback to consolation prize");
        }
    }
}
