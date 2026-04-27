package org.example.raffle.repository;

import org.example.raffle.domain.AwardTask;

import java.util.List;
import java.util.Optional;

public interface AwardTaskRepository {

    AwardTask save(AwardTask task);

    Optional<AwardTask> findById(Long taskId);

    List<AwardTask> findStaleTasks(List<String> statuses, int staleSeconds, int limit);

    int updateStatus(Long taskId, String currentStatus, String newStatus, int currentVersion);

    int markFailed(Long taskId, int currentVersion, String failReason);
}
