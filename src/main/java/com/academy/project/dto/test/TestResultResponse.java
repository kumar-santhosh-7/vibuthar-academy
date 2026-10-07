package com.academy.project.dto.test;

import com.academy.project.entity.test.OnlineTest;
import com.academy.project.entity.test.TestAttempt;
import com.academy.project.enums.AttemptStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TestResultResponse {

    private Long attemptId;
    private String testId;
    private String userId;
    private AttemptStatus status;

    private Integer numberOfQuestions;
    private Integer totalMarks;
    /** Allowed duration for the test, in minutes. */
    private Integer totalTimeMinutes;
    private Integer cutOff;

    private Integer correctAnswers;
    private Integer incorrectAnswers;
    private Integer marksObtained;
    private Double percentage;
    private Boolean passed;

    /** Actual time the candidate took, in seconds. */
    private Integer timeTakenSeconds;
    /** Human-friendly mm:ss of time taken. */
    private String timeTakenFormatted;

    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    public static TestResultResponse from(TestAttempt attempt, OnlineTest test) {
        Integer numberOfQuestions = attempt.getTotalQuestions();
        Integer totalMarks = resolveTotalMarks(test, numberOfQuestions);
        Integer cutOff = test.getCutOff();
        Integer marksObtained = attempt.getMarksObtained();
        Boolean passed = null;
        if (cutOff != null && marksObtained != null) {
            passed = marksObtained >= cutOff;
        }

        return TestResultResponse.builder()
                .attemptId(attempt.getId())
                .testId(test.getTestId())
                .userId(attempt.getUserId())
                .status(attempt.getStatus())
                .numberOfQuestions(numberOfQuestions)
                .totalMarks(totalMarks)
                .totalTimeMinutes(test.getDurationMinutes())
                .cutOff(cutOff)
                .correctAnswers(attempt.getCorrectAnswers())
                .incorrectAnswers(attempt.getIncorrectAnswers())
                .marksObtained(marksObtained)
                .percentage(attempt.getPercentage())
                .passed(passed)
                .timeTakenSeconds(attempt.getTimeTakenSeconds())
                .timeTakenFormatted(formatDuration(attempt.getTimeTakenSeconds()))
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }

    private static Integer resolveTotalMarks(OnlineTest test, Integer numberOfQuestions) {
        if (test.getTotalMarks() != null) {
            return test.getTotalMarks();
        }
        return numberOfQuestions;
    }

    private static String formatDuration(Integer totalSeconds) {
        if (totalSeconds == null) {
            return null;
        }
        int seconds = Math.max(0, totalSeconds);
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%d:%02d", minutes, secs);
    }
}
