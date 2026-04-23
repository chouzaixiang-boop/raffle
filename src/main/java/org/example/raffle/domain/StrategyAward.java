package org.example.raffle.domain;

import java.math.BigDecimal;
import java.util.List;

public record StrategyAward(Long strategyId,
                            Long awardId,
                            String awardTitle,
                            List<String> ruleModels,
                            int awardAllocate,
                            int awardSurplus,
                            BigDecimal awardRate,
                            int awardIndex) {
}
