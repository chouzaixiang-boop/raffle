package org.example.raffle.service.impl;

import org.example.raffle.domain.AwardTask;
import org.example.raffle.domain.RaffleContext;
import org.example.raffle.domain.RaffleRecord;
import org.example.raffle.domain.RaffleResult;
import org.example.raffle.repository.AwardTaskRepository;
import org.example.raffle.repository.RaffleRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RaffleDrawTransactionService {

    private final RaffleRecordRepository raffleRecordRepository;
    private final AwardTaskRepository awardTaskRepository;

    public RaffleDrawTransactionService(RaffleRecordRepository raffleRecordRepository,
                                        AwardTaskRepository awardTaskRepository) {
        this.raffleRecordRepository = raffleRecordRepository;
        this.awardTaskRepository = awardTaskRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public RaffleResult persist(RaffleContext context) {
        Long taskId = null;
        if (context.isSuccess()) {
            AwardTask savedTask = awardTaskRepository.save(new AwardTask(
                    null,
                    context.getUserId(),
                    context.getStrategyId(),
                    context.getAwardId(),
                    context.getAwardName(),
                    "PENDING",
                    0,
                    Instant.now()
            ));
            taskId = savedTask.taskId();
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
}