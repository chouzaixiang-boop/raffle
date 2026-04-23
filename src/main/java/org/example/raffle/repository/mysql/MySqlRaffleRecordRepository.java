package org.example.raffle.repository.mysql;

import org.example.raffle.domain.RaffleRecord;
import org.example.raffle.repository.RaffleRecordRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
@Profile("!local")
public class MySqlRaffleRecordRepository implements RaffleRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public MySqlRaffleRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(RaffleRecord record) {
        jdbcTemplate.update(
                "insert into raffle_record(user_id, strategy_id, award_id, award_name, success, message, create_time) values(?, ?, ?, ?, ?, ?, ?)",
                record.userId(),
                record.strategyId(),
                record.awardId(),
                record.awardName(),
                record.success(),
                record.message(),
                Timestamp.from(record.createTime())
        );
    }

    @Override
    public List<RaffleRecord> findAll() {
        return jdbcTemplate.query(
                "select user_id, strategy_id, award_id, award_name, success, message, create_time from raffle_record order by id desc",
                (resultSet, rowNum) -> new RaffleRecord(
                        resultSet.getLong("user_id"),
                        resultSet.getLong("strategy_id"),
                        resultSet.getLong("award_id"),
                        resultSet.getString("award_name"),
                        resultSet.getBoolean("success"),
                        resultSet.getString("message"),
                        resultSet.getTimestamp("create_time").toInstant()
                )
        );
    }
}
