package com.academy.project.entity.test;

import com.academy.project.enums.AttemptStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "test_attempts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_test_attempt_user",
                columnNames = {"test_pk", "user_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK to tests.id (internal PK). */
    @Column(name = "test_pk", nullable = false)
    private Long testPk;

    /** Public business user id (e.g. STU000001). */
    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    /** Correct answers count (set on submit). */
    @Column
    private Integer score;

    @Column(name = "correct_answers")
    private Integer correctAnswers;

    @Column(name = "incorrect_answers")
    private Integer incorrectAnswers;

    @Column(name = "marks_obtained")
    private Integer marksObtained;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column
    private Double percentage;

    /** Seconds between start and submit. */
    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = AttemptStatus.IN_PROGRESS;
        }
    }
}
