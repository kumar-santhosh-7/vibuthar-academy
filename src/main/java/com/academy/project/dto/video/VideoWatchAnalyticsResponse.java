package com.academy.project.dto.video;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class VideoWatchAnalyticsResponse {

    private Long videoId;
    private String courseId;
    private String title;
    private Integer sortOrder;
    private Integer durationMinutes;
    private long totalSubscribers;
    private long watchedCount;
    private long unwatchedCount;
    private BigDecimal watchRatePercent;
}
