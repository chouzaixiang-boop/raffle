package org.example.raffle.domain;

public record RuleConfig(Long strategyId,
                         Long awardId,
                         String ruleModel,
                         String ruleValue,
                         String ruleDesc) {
}