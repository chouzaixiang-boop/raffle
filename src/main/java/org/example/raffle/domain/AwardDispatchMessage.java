package org.example.raffle.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public record AwardDispatchMessage(Long taskId,
                                   Long userId,
                                   Long strategyId,
                                   Long awardId,
                                   String awardName,
                                   int version) {

    public static AwardDispatchMessage fromTask(AwardTask task) {
        return new AwardDispatchMessage(
                task.taskId(),
                task.userId(),
                task.strategyId(),
                task.awardId(),
                task.awardName(),
                task.version()
        );
    }

    public Map<String, String> toMap() {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("taskId", String.valueOf(taskId));
        payload.put("userId", String.valueOf(userId));
        payload.put("strategyId", String.valueOf(strategyId));
        payload.put("awardId", String.valueOf(awardId));
        payload.put("awardName", awardName == null ? "" : awardName);
        payload.put("version", String.valueOf(version));
        return payload;
    }

    public static AwardDispatchMessage fromMap(Map<Object, Object> values) {
        return new AwardDispatchMessage(
                Long.parseLong(String.valueOf(values.get("taskId"))),
                Long.parseLong(String.valueOf(values.get("userId"))),
                Long.parseLong(String.valueOf(values.get("strategyId"))),
                Long.parseLong(String.valueOf(values.get("awardId"))),
                String.valueOf(values.get("awardName")),
                Integer.parseInt(String.valueOf(values.get("version")))
        );
    }
}
