package com.academy.project.dto.test;

import com.academy.project.entity.test.OnlineTest;
import com.academy.project.enums.TestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TestResponse {

    private Long id;
    private String testId;
    private String title;
    private String description;
    private String pdfUrl;
    private Integer durationMinutes;
    private Integer totalMarks;
    private Integer cutOff;
    private TestStatus status;
    private Integer questionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<QuestionResponse> questions;

    public static TestResponse summary(OnlineTest test, int questionCount) {
        return TestResponse.builder()
                .id(test.getId())
                .testId(test.getTestId())
                .title(test.getTitle())
                .description(test.getDescription())
                .pdfUrl(test.getPdfUrl())
                .durationMinutes(test.getDurationMinutes())
                .totalMarks(test.getTotalMarks() != null ? test.getTotalMarks() : questionCount)
                .cutOff(test.getCutOff())
                .status(test.getStatus())
                .questionCount(questionCount)
                .createdAt(test.getCreatedAt())
                .updatedAt(test.getUpdatedAt())
                .build();
    }

    public static TestResponse withQuestions(OnlineTest test, List<QuestionResponse> questions) {
        int count = questions != null ? questions.size() : 0;
        return TestResponse.builder()
                .id(test.getId())
                .testId(test.getTestId())
                .title(test.getTitle())
                .description(test.getDescription())
                .pdfUrl(test.getPdfUrl())
                .durationMinutes(test.getDurationMinutes())
                .totalMarks(test.getTotalMarks() != null ? test.getTotalMarks() : count)
                .cutOff(test.getCutOff())
                .status(test.getStatus())
                .questionCount(count)
                .createdAt(test.getCreatedAt())
                .updatedAt(test.getUpdatedAt())
                .questions(questions)
                .build();
    }
}
