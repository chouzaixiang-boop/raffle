package org.example.raffle.service.impl;

import org.example.raffle.domain.AwardDispatchMessage;
import org.example.raffle.domain.AwardTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AwardStreamPublisher {

    private static final Logger log = LoggerFactory.getLogger(AwardStreamPublisher.class);
    private static final String STREAM_KEY = "raffle:award:stream";
    private static final String GROUP_NAME = "raffle-award-group";

    private final StringRedisTemplate redisTemplate;

    public AwardStreamPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(AwardTask task) {
        if (task == null || task.taskId() == null) {
            return;
        }
        ensureGroupExists();
        AwardDispatchMessage message = AwardDispatchMessage.fromTask(task);
        redisTemplate.opsForStream().add(STREAM_KEY, message.toMap());
        log.info("raffle_award_stream_publish taskId={} userId={} strategyId={} awardId={}",
                task.taskId(), task.userId(), task.strategyId(), task.awardId());
    }

    private void ensureGroupExists() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, org.springframework.data.redis.connection.stream.ReadOffset.from("0"), GROUP_NAME);
        } catch (Exception ex) {
            if (log.isDebugEnabled()) {
                log.debug("raffle_award_stream_group_ready streamKey={} groupName={} reason={}", STREAM_KEY, GROUP_NAME, ex.getMessage());
            }
        }
    }
}
