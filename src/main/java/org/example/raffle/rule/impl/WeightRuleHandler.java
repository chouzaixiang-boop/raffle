package org.example.raffle.rule.impl;

import org.example.raffle.domain.RaffleContext;
import org.example.raffle.repository.RuleRepository;
import org.example.raffle.rule.RuleHandler;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class WeightRuleHandler implements RuleHandler {

    private static final String RULE_MODEL = "rule_weight";
    private final RuleRepository ruleRepository;

    public WeightRuleHandler(RuleRepository ruleRepository) {
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
        String[] segments = ruleValue.split(";");
        if (segments.length != 2) {
            return;
        }
        Set<Long> weightedUsers = Arrays.stream(segments[0].split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
        Long weightedStrategyId = Long.valueOf(segments[1].trim());
        if (weightedUsers.contains(context.getUserId())) {
            context.setActualStrategyId(weightedStrategyId);
            context.setMessage("weighted strategy selected");
        }
    }
}
