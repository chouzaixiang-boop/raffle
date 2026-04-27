package org.example.raffle.repository.mysql;

import org.example.raffle.domain.RaffleRefund;
import org.example.raffle.repository.RaffleRefundRepository;
import org.example.raffle.repository.mysql.mapper.RaffleRefundMapper;
import org.example.raffle.repository.mysql.po.RaffleRefundRow;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.ZoneId;
import java.util.Optional;

@Repository
@Profile("!local")
public class MySqlRaffleRefundRepository implements RaffleRefundRepository {

    private final RaffleRefundMapper raffleRefundMapper;

    public MySqlRaffleRefundRepository(RaffleRefundMapper raffleRefundMapper) {
        this.raffleRefundMapper = raffleRefundMapper;
    }

    @Override
    public Optional<RaffleRefund> findByRefundId(String refundId) {
        RaffleRefundRow row = raffleRefundMapper.findByRefundId(refundId);
        return row == null ? Optional.empty() : Optional.of(toDomain(row));
    }

    @Override
    public Optional<RaffleRefund> findByTaskId(Long taskId) {
        RaffleRefundRow row = raffleRefundMapper.findByTaskId(taskId);
        return row == null ? Optional.empty() : Optional.of(toDomain(row));
    }

    @Override
    public RaffleRefund save(RaffleRefund raffleRefund) {
        RaffleRefundRow row = new RaffleRefundRow();
        row.setId(raffleRefund.id());
        row.setRefundId(raffleRefund.refundId());
        row.setTaskId(raffleRefund.taskId());
        row.setUserId(raffleRefund.userId());
        row.setStrategyId(raffleRefund.strategyId());
        row.setAwardId(raffleRefund.awardId());
        row.setRefundStatus(raffleRefund.refundStatus());
        row.setRefundMessage(raffleRefund.refundMessage());
        row.setCreateTime(raffleRefund.createTime().atZone(ZoneId.systemDefault()).toLocalDateTime());
        raffleRefundMapper.insert(row);
        return toDomain(row);
    }

    private RaffleRefund toDomain(RaffleRefundRow row) {
        return new RaffleRefund(
                row.getId(),
                row.getRefundId(),
                row.getTaskId(),
                row.getUserId(),
                row.getStrategyId(),
                row.getAwardId(),
                row.getRefundStatus(),
                row.getRefundMessage(),
                row.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()
        );
    }
}
