package org.example.raffle.service.impl;

import org.example.raffle.domain.AwardDispatchMessage;
import org.example.raffle.domain.AwardReceived;
import org.example.raffle.domain.AwardTask;
import org.example.raffle.repository.AwardReceivedRepository;
import org.example.raffle.repository.AwardTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AwardTaskProcessService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_AWARDED = "AWARDED";

    private final AwardTaskRepository awardTaskRepository;
    private final AwardReceivedRepository awardReceivedRepository;

    public AwardTaskProcessService(AwardTaskRepository awardTaskRepository,
                                   AwardReceivedRepository awardReceivedRepository) {
        this.awardTaskRepository = awardTaskRepository;
        this.awardReceivedRepository = awardReceivedRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean process(AwardDispatchMessage message) {
        if (message == null || message.taskId() == null) {
            return true;
        }

        AwardTask task = awardTaskRepository.findById(message.taskId()).orElse(null);
        if (task == null) {
            return true;
        }
        if (STATUS_AWARDED.equals(task.taskStatus())) {
            ensureAwardReceived(task);
            return true;
        }

        if (STATUS_PENDING.equals(task.taskStatus())) {
            int processingUpdated = awardTaskRepository.updateStatus(task.taskId(), STATUS_PENDING, STATUS_PROCESSING, task.version());
            if (processingUpdated <= 0) {
                return false;
            }
            task = awardTaskRepository.findById(task.taskId()).orElse(task);
        }

        if (STATUS_PROCESSING.equals(task.taskStatus())) {
            int awardedUpdated = awardTaskRepository.updateStatus(task.taskId(), STATUS_PROCESSING, STATUS_AWARDED, task.version());
            if (awardedUpdated > 0) {
                ensureAwardReceived(task);
            }
            return awardedUpdated > 0;
        }

        return true;
    }

    private void ensureAwardReceived(AwardTask task) {
        if (awardReceivedRepository.findByTaskId(task.taskId()).isPresent()) {
            return;
        }
        awardReceivedRepository.save(new AwardReceived(
                null,
                task.taskId(),
                task.userId(),
                task.strategyId(),
                task.awardId(),
                task.awardName(),
                STATUS_AWARDED,
                Instant.now()
        ));
    }
}
