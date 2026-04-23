package org.example.raffle.repository.mysql;

import org.example.raffle.domain.Award;
import org.example.raffle.repository.AwardRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
@Profile("!local")
public class MySqlAwardRepository implements AwardRepository {

    private final JdbcTemplate jdbcTemplate;

    public MySqlAwardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Award> findById(Long awardId) {
        return jdbcTemplate.query(
                "select award_id, award_type, award_name, award_value, award_desc from award where award_id = ?",
                (resultSet, rowNum) -> new Award(
                        resultSet.getLong("award_id"),
                        resultSet.getString("award_name"),
                        String.valueOf(resultSet.getInt("award_type")),
                        resultSet.getString("award_value"),
                        resultSet.getString("award_desc")
                ),
                awardId
        ).stream().findFirst();
    }

    @Override
    public Map<Long, Award> findAll() {
        Map<Long, Award> awards = new LinkedHashMap<>();
        jdbcTemplate.query(
                "select award_id, award_type, award_name, award_value, award_desc from award order by id",
            (resultSet, rowNum) -> awards.put(resultSet.getLong("award_id"), new Award(
                        resultSet.getLong("award_id"),
                        resultSet.getString("award_name"),
                        String.valueOf(resultSet.getInt("award_type")),
                        resultSet.getString("award_value"),
                        resultSet.getString("award_desc")
                ))
        );
        return awards;
    }
}
