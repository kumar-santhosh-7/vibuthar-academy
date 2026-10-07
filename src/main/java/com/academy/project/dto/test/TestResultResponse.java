package com.academy.project.dto.test;

import com.academy.project.entity.test.TestAttempt;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TestResultResponse {

    private Long attemptId;
    private String testId;
    private String userId;
    private Integer score;
    private Integer totalQuestions;
    private Double percentage;
    private LocalDateTime submittedAt;

    public static TestResultResponse from(TestAttempt attempt, String testId) {
        return TestResultResponse.builder()
                .attemptId(attempt.getId())
                .testId(testId)
                .userId(attempt.getUserId())
                .score(attempt.getScore())
                .totalQuestions(attempt.getTotalQuestions())
                .percentage(attempt.getPercentage())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }
}
