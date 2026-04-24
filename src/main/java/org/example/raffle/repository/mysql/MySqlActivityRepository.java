package org.example.raffle.repository.mysql;

import org.example.raffle.domain.Activity;
import org.example.raffle.repository.ActivityRepository;
import org.example.raffle.repository.mysql.mapper.ActivityMapper;
import org.example.raffle.repository.mysql.po.ActivityRow;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("!local")
public class MySqlActivityRepository implements ActivityRepository {

    private final ActivityMapper activityMapper;

    public MySqlActivityRepository(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
    }

    @Override
    public Optional<Activity> findById(Long activityId) {
        return Optional.ofNullable(activityMapper.findByActivityId(activityId)).map(this::toDomain);
    }

    @Override
    public List<Activity> findAllEnabled() {
        return activityMapper.findAllEnabled().stream().map(this::toDomain).toList();
    }

    private Activity toDomain(ActivityRow row) {
        return new Activity(
                row.getActivityId(),
                row.getActivityName(),
                row.getActivityDesc(),
                row.getStrategyId(),
                row.getPageTitle(),
                row.getPageSubtitle(),
                row.getBannerUrl(),
                row.getThemeColor(),
                row.getSortNo(),
                row.getStatus()
        );
    }
}