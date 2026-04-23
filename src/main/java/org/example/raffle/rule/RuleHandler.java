package org.example.raffle.rule;

import org.example.raffle.domain.RaffleContext;

public interface RuleHandler {

    boolean supports(String ruleModel);

    void apply(RaffleContext context);
}
