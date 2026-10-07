package com.academy.project.dto.video;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CandidateVideoProgressSummary {

    private String userId;
    private String name;
    private String email;
    private String phone;
    private String courseId;
    private long totalVideos;
    private long watchedCount;
    private long unwatchedCount;
    private BigDecimal progressPercent;
}
