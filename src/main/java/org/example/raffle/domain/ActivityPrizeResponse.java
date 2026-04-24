package org.example.raffle.domain;

import java.math.BigDecimal;

public record ActivityPrizeResponse(Long awardId,
                                    String awardName,
                                    String awardTitle,
                                    BigDecimal awardRate,
                                    Integer awardAllocate,
                                    Integer awardSurplus,
                                    Integer awardIndex) {
}