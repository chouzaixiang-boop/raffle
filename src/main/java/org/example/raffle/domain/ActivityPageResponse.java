package org.example.raffle.domain;

import java.util.List;

public record ActivityPageResponse(Long activityId,
                                   String activityName,
                                   String activityDesc,
                                   Long strategyId,
                                   String strategyDesc,
                                   String pageTitle,
                                   String pageSubtitle,
                                   String bannerUrl,
                                   String themeColor,
                                   List<ActivityPrizeResponse> prizes) {
}