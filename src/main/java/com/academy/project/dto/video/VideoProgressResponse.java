package com.academy.project.dto.video;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class VideoProgressResponse {

    private Long videoId;
    private String courseId;
    private String title;
    private String videoUrl;
    private Integer sortOrder;
    private Integer durationMinutes;
    private Integer watchedSeconds;
    private Integer durationSeconds;
    private BigDecimal progressPercent;
    private boolean watched;
    private LocalDateTime lastWatchedAt;
}
