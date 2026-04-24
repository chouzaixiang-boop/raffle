package org.example.raffle.repository.memory;

import org.example.raffle.domain.Activity;
import org.example.raffle.repository.ActivityRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@Profile("local")
public class InMemoryActivityRepository implements ActivityRepository {

    private final Map<Long, Activity> activities = new LinkedHashMap<>();

    public void put(Activity activity) {
        activities.put(activity.activityId(), activity);
    }

    @Override
    public Optional<Activity> findById(Long activityId) {
        return Optional.ofNullable(activities.get(activityId));
    }

    @Override
    public List<Activity> findAllEnabled() {
        return activities.values().stream()
                .filter(activity -> activity.status() != null && activity.status() == 1)
                .sorted(Comparator.comparing(Activity::sortNo, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }
}