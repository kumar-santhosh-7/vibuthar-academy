package com.academy.project.dto.video;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CandidateVideoProgressDetail {

    private String userId;
    private String name;
    private String email;
    private String phone;
    private String courseId;
    private long totalVideos;
    private long watchedCount;
    private long unwatchedCount;
    private BigDecimal progressPercent;
    private List<VideoProgressResponse> videos;
}
