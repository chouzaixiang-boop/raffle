package org.example.raffle.domain;

import java.time.Instant;

public record AwardReceived(Long receivedId,
                            Long taskId,
                            Long userId,
                            Long strategyId,
                            Long awardId,
                            String awardName,
                            String receiveStatus,
                            Instant receiveTime) {
}
