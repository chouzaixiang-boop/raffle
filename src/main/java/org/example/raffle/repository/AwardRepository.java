package org.example.raffle.repository;

import org.example.raffle.domain.Award;

import java.util.Map;
import java.util.Optional;

public interface AwardRepository {

    Optional<Award> findById(Long awardId);

    Map<Long, Award> findAll();
}
