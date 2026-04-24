package org.example.raffle.repository;

import org.example.raffle.domain.Activity;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository {

    Optional<Activity> findById(Long activityId);

    List<Activity> findAllEnabled();
}