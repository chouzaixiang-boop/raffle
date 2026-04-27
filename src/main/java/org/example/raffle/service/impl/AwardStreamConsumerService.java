package org.example.raffle.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.example.raffle.domain.AwardDispatchMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AwardStreamConsumerService {

    private static final Logger log = LoggerFactory.getLogger(AwardStreamConsumerService.class);
    private static final String STREAM_KEY = "raffle:award:stream";
    private static final String GROUP_NAME = "raffle-award-group";
    private static final int BATCH_SIZE = 20;

    private final StringRedisTemplate redisTemplate;
    private final AwardTaskProcessService awardTaskProcessService;
    private final String consumerName = "award-consumer-" + Long.toHexString(System.nanoTime());

    private final Counter consumedCounter;
    private final Counter acknowledgedCounter;
    private final Counter failedCounter;
    private final Counter skippedCounter;
    private final Counter pollErrorCounter;
    private final Timer processLatencyTimer;
    private final AtomicLong pendingGauge;

    public AwardStreamConsumerService(StringRedisTemplate redisTemplate,
                                      AwardTaskProcessService awardTaskProcessService,
                                      MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.awardTaskProcessService = awardTaskProcessService;
        this.consumedCounter = meterRegistry.counter("raffle.award.stream.consumed.total");
        this.acknowledgedCounter = meterRegistry.counter("raffle.award.stream.acked.total");
        this.failedCounter = meterRegistry.counter("raffle.award.stream.failed.total");
        this.skippedCounter = meterRegistry.counter("raffle.award.stream.skipped.total");
        this.pollErrorCounter = meterRegistry.counter("raffle.award.stream.poll.error.total");
        this.processLatencyTimer = meterRegistry.timer("raffle.award.stream.process.latency");
        this.pendingGauge = meterRegistry.gauge("raffle.award.stream.pending.count", new AtomicLong(0));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initGroup() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
        } catch (Exception ex) {
            if (log.isDebugEnabled()) {
                log.debug("raffle_award_stream_group_init skipped streamKey={} groupName={} reason={}", STREAM_KEY, GROUP_NAME, ex.getMessage());
            }
        }
    }

    @Scheduled(fixedDelayString = "${raffle.award.stream.poll-interval-ms:500}")
    public void pollAwardMessages() {
        try {
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                    Consumer.from(GROUP_NAME, consumerName),
                    StreamReadOptions.empty().count(BATCH_SIZE).block(Duration.ofMillis(200)),
                    StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
            );
            if (records == null || records.isEmpty()) {
                return;
            }
            for (MapRecord<String, Object, Object> record : records) {
                consumedCounter.increment();
                boolean processed = false;
                Timer.Sample sample = Timer.start();
                try {
                    AwardDispatchMessage message = AwardDispatchMessage.fromMap(record.getValue());
                    processed = awardTaskProcessService.process(message);
                    if (processed) {
                        redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
                        acknowledgedCounter.increment();
                        log.info("raffle_award_stream_consume_ok messageId={} taskId={}", record.getId(), message.taskId());
                    }
                } catch (Exception ex) {
                    failedCounter.increment();
                    log.error("raffle_award_stream_consume_failed messageId={} error={}", record.getId(), ex.getMessage(), ex);
                } finally {
                    sample.stop(processLatencyTimer);
                }
                if (!processed) {
                    skippedCounter.increment();
                    log.warn("raffle_award_stream_consume_skip messageId={}", record.getId());
                }
            }
        } catch (Exception ex) {
            pollErrorCounter.increment();
            log.error("raffle_award_stream_poll_failed error={}", ex.getMessage(), ex);
        }
    }

    @Scheduled(fixedDelayString = "${raffle.award.stream.monitor-interval-ms:10000}")
    public void monitorPendingBacklog() {
        try {
            PendingMessagesSummary summary = redisTemplate.opsForStream().pending(STREAM_KEY, GROUP_NAME);
            long pending = summary == null ? 0 : summary.getTotalPendingMessages();
            pendingGauge.set(pending);
            if (pending > 0) {
                log.warn("raffle_award_stream_backlog pending={} group={} consumer={}", pending, GROUP_NAME, consumerName);
            }
        } catch (Exception ex) {
            pollErrorCounter.increment();
            log.error("raffle_award_stream_backlog_monitor_failed error={}", ex.getMessage(), ex);
        }
    }
}
