package org.example.raffle.domain;

import java.time.Instant;

public record AwardTask(Long taskId,
                        Long userId,
                        Long strategyId,
                        Long awardId,
                        String awardName,
                        String taskStatus,
                        int version,
                        int retryCount,
                        String failReason,
                        Instant createTime,
                        Instant updateTime) {
}
