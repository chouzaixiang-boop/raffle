package org.example.raffle.repository.mysql;

import org.example.raffle.domain.Award;
import org.example.raffle.repository.AwardRepository;
import org.example.raffle.repository.mysql.mapper.AwardMapper;
import org.example.raffle.repository.mysql.po.AwardRow;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
@Profile("!local")
public class MySqlAwardRepository implements AwardRepository {

    private final AwardMapper awardMapper;

    public MySqlAwardRepository(AwardMapper awardMapper) {
        this.awardMapper = awardMapper;
    }

    @Override
    public Optional<Award> findById(Long awardId) {
        return Optional.ofNullable(awardMapper.findByAwardId(awardId)).map(this::toDomain);
    }

    @Override
    public Map<Long, Award> findAll() {
        Map<Long, Award> awards = new LinkedHashMap<>();
        awardMapper.findAll().forEach(row -> awards.put(row.getAwardId(), toDomain(row)));
        return awards;
    }

    private Award toDomain(AwardRow row) {
        return new Award(
                row.getAwardId(),
                row.getAwardName(),
                String.valueOf(row.getAwardType()),
                row.getAwardValue(),
                row.getAwardDesc()
        );
    }
}
