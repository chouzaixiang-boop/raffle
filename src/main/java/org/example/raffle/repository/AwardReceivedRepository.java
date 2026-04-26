package org.example.raffle.repository;

import org.example.raffle.domain.AwardReceived;

import java.util.Optional;

public interface AwardReceivedRepository {

    AwardReceived save(AwardReceived awardReceived);

    Optional<AwardReceived> findByTaskId(Long taskId);
}
