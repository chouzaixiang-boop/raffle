package org.example.raffle.repository;

import org.example.raffle.domain.AwardTask;

import java.util.Optional;

public interface AwardTaskRepository {

    AwardTask save(AwardTask task);

    Optional<AwardTask> findById(Long taskId);

    int updateStatus(Long taskId, String currentStatus, String newStatus, int currentVersion);
}
