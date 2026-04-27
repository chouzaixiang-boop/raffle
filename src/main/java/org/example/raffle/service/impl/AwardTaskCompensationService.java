package org.example.raffle.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.example.raffle.domain.AwardTask;
import org.example.raffle.repository.AwardTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AwardTaskCompensationService {

    private static final Logger log = LoggerFactory.getLogger(AwardTaskCompensationService.class);

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";

    private final AwardTaskRepository awardTaskRepository;
    private final AwardStreamPublisher awardStreamPublisher;

    private final Counter scanCounter;
    private final Counter republishCounter;
    private final Counter resetCounter;
    private final Counter failureCounter;

    @Value("${raffle.award.compensation.stale-seconds:30}")
    private int staleSeconds;

    @Value("${raffle.award.compensation.batch-size:100}")
    private int batchSize;

    public AwardTaskCompensationService(AwardTaskRepository awardTaskRepository,
                                        AwardStreamPublisher awardStreamPublisher,
                                        MeterRegistry meterRegistry) {
        this.awardTaskRepository = awardTaskRepository;
        this.awardStreamPublisher = awardStreamPublisher;
        this.scanCounter = meterRegistry.counter("raffle.award.compensation.scan.total");
        this.republishCounter = meterRegistry.counter("raffle.award.compensation.republish.total");
        this.resetCounter = meterRegistry.counter("raffle.award.compensation.reset.total");
        this.failureCounter = meterRegistry.counter("raffle.award.compensation.failure.total");
    }

    @Scheduled(fixedDelayString = "${raffle.award.compensation.interval-ms:5000}")
    public void compensateStaleTasks() {
        List<AwardTask> staleTasks = awardTaskRepository.findStaleTasks(
                List.of(STATUS_PENDING, STATUS_PROCESSING),
            staleSeconds,
            batchSize
        );
        scanCounter.increment(staleTasks.size());

        if (staleTasks.isEmpty()) {
            return;
        }

        int republished = 0;
        int resetToPending = 0;

        for (AwardTask task : staleTasks) {
            try {
                AwardTask toPublish = task;
                if (STATUS_PROCESSING.equals(task.taskStatus())) {
                    int updated = awardTaskRepository.updateStatus(task.taskId(), STATUS_PROCESSING, STATUS_PENDING, task.version());
                    if (updated <= 0) {
                        continue;
                    }
                    resetCounter.increment();
                    resetToPending++;
                    toPublish = awardTaskRepository.findById(task.taskId()).orElse(task);
                }

                if (!STATUS_PENDING.equals(toPublish.taskStatus())) {
                    continue;
                }

                awardStreamPublisher.publish(toPublish);
                republishCounter.increment();
                republished++;
            } catch (Exception ex) {
                failureCounter.increment();
                log.error("raffle_award_compensation_failed taskId={} error={}", task.taskId(), ex.getMessage(), ex);
            }
        }

        log.info("raffle_award_compensation_tick stale={} republished={} reset={}",
                staleTasks.size(),
                republished,
                resetToPending);
    }
}
