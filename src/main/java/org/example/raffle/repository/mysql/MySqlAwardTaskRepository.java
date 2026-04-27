package org.example.raffle.repository.mysql;

import org.example.raffle.domain.AwardTask;
import org.example.raffle.repository.AwardTaskRepository;
import org.example.raffle.repository.mysql.mapper.AwardTaskMapper;
import org.example.raffle.repository.mysql.po.AwardTaskRow;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!local")
public class MySqlAwardTaskRepository implements AwardTaskRepository {

    private final AwardTaskMapper awardTaskMapper;

    public MySqlAwardTaskRepository(AwardTaskMapper awardTaskMapper) {
        this.awardTaskMapper = awardTaskMapper;
    }

    @Override
    public AwardTask save(AwardTask task) {
        AwardTaskRow row = new AwardTaskRow();
        row.setTaskId(task.taskId());
        row.setUserId(task.userId());
        row.setStrategyId(task.strategyId());
        row.setAwardId(task.awardId());
        row.setAwardName(task.awardName());
        row.setTaskStatus(task.taskStatus());
        row.setVersion(task.version());
        row.setRetryCount(task.retryCount());
        row.setFailReason(task.failReason());
        row.setCreateTime(task.createTime().atZone(ZoneId.systemDefault()).toLocalDateTime());
        row.setUpdateTime(task.updateTime().atZone(ZoneId.systemDefault()).toLocalDateTime());
        awardTaskMapper.insert(row);
        return new AwardTask(
                row.getTaskId(),
                row.getUserId(),
                row.getStrategyId(),
                row.getAwardId(),
                row.getAwardName(),
                row.getTaskStatus(),
                row.getVersion() == null ? 0 : row.getVersion(),
            row.getRetryCount() == null ? 0 : row.getRetryCount(),
            row.getFailReason(),
            row.getCreateTime().atZone(ZoneId.systemDefault()).toInstant(),
            row.getUpdateTime().atZone(ZoneId.systemDefault()).toInstant()
        );
    }

    @Override
    public Optional<AwardTask> findById(Long taskId) {
        AwardTaskRow row = awardTaskMapper.findById(taskId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(row));
    }

    @Override
    public List<AwardTask> findStaleTasks(List<String> statuses, int staleSeconds, int limit) {
        if (statuses == null || statuses.isEmpty() || staleSeconds <= 0 || limit <= 0) {
            return List.of();
        }
        return awardTaskMapper.findStaleTasks(statuses, staleSeconds, limit)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int updateStatus(Long taskId, String currentStatus, String newStatus, int currentVersion) {
        return awardTaskMapper.updateStatus(taskId, currentStatus, newStatus, currentVersion);
    }

    @Override
    public int markFailed(Long taskId, int currentVersion, String failReason) {
        return awardTaskMapper.markFailed(taskId, currentVersion, failReason);
    }

    private AwardTask toDomain(AwardTaskRow row) {
        return new AwardTask(
                row.getTaskId(),
                row.getUserId(),
                row.getStrategyId(),
                row.getAwardId(),
                row.getAwardName(),
                row.getTaskStatus(),
                row.getVersion() == null ? 0 : row.getVersion(),
                row.getRetryCount() == null ? 0 : row.getRetryCount(),
                row.getFailReason(),
                row.getCreateTime().atZone(ZoneId.systemDefault()).toInstant(),
                row.getUpdateTime().atZone(ZoneId.systemDefault()).toInstant()
        );
    }
}
