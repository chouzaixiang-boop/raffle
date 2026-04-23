package org.example.raffle.domain;

import java.util.List;

public record Strategy(Long strategyId, String strategyDesc, List<String> ruleModels) {
}
