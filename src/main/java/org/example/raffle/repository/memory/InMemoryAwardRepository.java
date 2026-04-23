package org.example.raffle.repository.memory;

import org.example.raffle.domain.Award;
import org.example.raffle.repository.AwardRepository;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
@Profile("local")
public class InMemoryAwardRepository implements AwardRepository {

    private final Map<Long, Award> awards = new LinkedHashMap<>();

    public void put(Award award) {
        awards.put(award.awardId(), award);
    }

    @Override
    public Optional<Award> findById(Long awardId) {
        return Optional.ofNullable(awards.get(awardId));
    }

    @Override
    public Map<Long, Award> findAll() {
        return Collections.unmodifiableMap(awards);
    }
}
