package org.example.raffle.repository.mysql;

import org.example.raffle.domain.RaffleRecord;
import org.example.raffle.repository.RaffleRecordRepository;
import org.example.raffle.repository.mysql.mapper.RaffleRecordMapper;
import org.example.raffle.repository.mysql.po.RaffleRecordRow;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.ZoneId;
import java.util.List;

@Repository
@Profile("!local")
public class MySqlRaffleRecordRepository implements RaffleRecordRepository {

    private final RaffleRecordMapper raffleRecordMapper;

    public MySqlRaffleRecordRepository(RaffleRecordMapper raffleRecordMapper) {
        this.raffleRecordMapper = raffleRecordMapper;
    }

    @Override
    public void save(RaffleRecord record) {
        RaffleRecordRow row = new RaffleRecordRow();
        row.setUserId(record.userId());
        row.setStrategyId(record.strategyId());
        row.setAwardId(record.awardId());
        row.setAwardName(record.awardName());
        row.setSuccess(record.success());
        row.setMessage(record.message());
        row.setCreateTime(record.createTime().atZone(ZoneId.systemDefault()).toLocalDateTime());
        raffleRecordMapper.insert(row);
    }

    @Override
    public List<RaffleRecord> findAll() {
        return raffleRecordMapper.findAll().stream().map(row -> new RaffleRecord(
                row.getUserId(),
                row.getStrategyId(),
                row.getAwardId(),
                row.getAwardName(),
                Boolean.TRUE.equals(row.getSuccess()),
                row.getMessage(),
                row.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()
        )).toList();
    }
}
