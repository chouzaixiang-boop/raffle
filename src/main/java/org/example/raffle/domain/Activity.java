package org.example.raffle.domain;

public record Activity(Long activityId,
                       String activityName,
                       String activityDesc,
                       Long strategyId,
                       String pageTitle,
                       String pageSubtitle,
                       String bannerUrl,
                       String themeColor,
                       Integer sortNo,
                       Integer status) {
}