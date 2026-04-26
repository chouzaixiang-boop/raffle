package org.example.raffle.repository.mysql;

import org.example.raffle.domain.AwardTask;
import org.example.raffle.repository.AwardTaskRepository;
import org.example.raffle.repository.mysql.mapper.AwardTaskMapper;
import org.example.raffle.repository.mysql.po.AwardTaskRow;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.ZoneId;
import java.util.Optional;

@Repository
@Profile("!local")
public class MySqlAwardTaskRepository implements AwardTaskRepository {

    private static final String TASK_STATUS_PENDING = "PENDING";

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
        row.setCreateTime(task.createTime().atZone(ZoneId.systemDefault()).toLocalDateTime());
        awardTaskMapper.insert(row);
        return new AwardTask(
                row.getTaskId(),
                row.getUserId(),
                row.getStrategyId(),
                row.getAwardId(),
                row.getAwardName(),
                row.getTaskStatus(),
                row.getVersion() == null ? 0 : row.getVersion(),
                row.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()
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
    public int updateStatus(Long taskId, String currentStatus, String newStatus, int currentVersion) {
        return awardTaskMapper.updateStatus(taskId, currentStatus, newStatus, currentVersion);
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
                row.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()
        );
    }
}
