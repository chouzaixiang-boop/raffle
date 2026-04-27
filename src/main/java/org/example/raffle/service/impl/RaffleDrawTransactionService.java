package org.example.raffle.service.impl;

import org.example.raffle.domain.AwardTask;
import org.example.raffle.domain.RaffleContext;
import org.example.raffle.domain.RaffleRecord;
import org.example.raffle.domain.RaffleResult;
import org.example.raffle.repository.AwardTaskRepository;
import org.example.raffle.repository.RaffleRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Service
public class RaffleDrawTransactionService {

    private final RaffleRecordRepository raffleRecordRepository;
    private final AwardTaskRepository awardTaskRepository;
    private final AwardStreamPublisher awardStreamPublisher;

    public RaffleDrawTransactionService(RaffleRecordRepository raffleRecordRepository,
                                        AwardTaskRepository awardTaskRepository,
                                        AwardStreamPublisher awardStreamPublisher) {
        this.raffleRecordRepository = raffleRecordRepository;
        this.awardTaskRepository = awardTaskRepository;
        this.awardStreamPublisher = awardStreamPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public RaffleResult persist(RaffleContext context) {
        Long taskId = null;
        if (context.isSuccess()) {
            Instant now = Instant.now();
            AwardTask savedTask = awardTaskRepository.save(new AwardTask(
                    null,
                    context.getUserId(),
                    context.getStrategyId(),
                    context.getAwardId(),
                    context.getAwardName(),
                    "PENDING",
                    0,
                    0,
                    null,
                    now,
                    now
            ));
            taskId = savedTask.taskId();
            registerAfterCommitPublish(savedTask);
        }

        RaffleResult result = new RaffleResult(
                context.getUserId(),
                context.getStrategyId(),
                context.getAwardId(),
                context.getAwardName(),
                taskId,
                context.isSuccess(),
                context.getMessage()
        );

        raffleRecordRepository.save(new RaffleRecord(
                context.getUserId(),
                context.getStrategyId(),
                context.getAwardId(),
                context.getAwardName(),
                context.isSuccess(),
                context.getMessage(),
                Instant.now()
        ));

        return result;
    }

    private void registerAfterCommitPublish(AwardTask savedTask) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            awardStreamPublisher.publish(savedTask);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                awardStreamPublisher.publish(savedTask);
            }
        });
    }
}