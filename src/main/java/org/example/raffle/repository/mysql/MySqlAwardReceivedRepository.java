package org.example.raffle.repository.mysql;

import org.example.raffle.domain.AwardReceived;
import org.example.raffle.repository.AwardReceivedRepository;
import org.example.raffle.repository.mysql.mapper.AwardReceivedMapper;
import org.example.raffle.repository.mysql.po.AwardReceivedRow;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.ZoneId;
import java.util.Optional;

@Repository
@Profile("!local")
public class MySqlAwardReceivedRepository implements AwardReceivedRepository {

    private final AwardReceivedMapper awardReceivedMapper;

    public MySqlAwardReceivedRepository(AwardReceivedMapper awardReceivedMapper) {
        this.awardReceivedMapper = awardReceivedMapper;
    }

    @Override
    public AwardReceived save(AwardReceived awardReceived) {
        AwardReceivedRow row = new AwardReceivedRow();
        row.setReceivedId(awardReceived.receivedId());
        row.setTaskId(awardReceived.taskId());
        row.setUserId(awardReceived.userId());
        row.setStrategyId(awardReceived.strategyId());
        row.setAwardId(awardReceived.awardId());
        row.setAwardName(awardReceived.awardName());
        row.setReceiveStatus(awardReceived.receiveStatus());
        row.setReceiveTime(awardReceived.receiveTime().atZone(ZoneId.systemDefault()).toLocalDateTime());
        row.setCreateTime(awardReceived.receiveTime().atZone(ZoneId.systemDefault()).toLocalDateTime());
        awardReceivedMapper.insert(row);
        return new AwardReceived(
                row.getReceivedId(),
                row.getTaskId(),
                row.getUserId(),
                row.getStrategyId(),
                row.getAwardId(),
                row.getAwardName(),
                row.getReceiveStatus(),
                row.getReceiveTime().atZone(ZoneId.systemDefault()).toInstant()
        );
    }

    @Override
    public Optional<AwardReceived> findByTaskId(Long taskId) {
        AwardReceivedRow row = awardReceivedMapper.findByTaskId(taskId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new AwardReceived(
                row.getReceivedId(),
                row.getTaskId(),
                row.getUserId(),
                row.getStrategyId(),
                row.getAwardId(),
                row.getAwardName(),
                row.getReceiveStatus(),
                row.getReceiveTime().atZone(ZoneId.systemDefault()).toInstant()
        ));
    }
}
