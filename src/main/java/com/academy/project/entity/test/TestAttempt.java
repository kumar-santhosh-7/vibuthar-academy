package com.academy.project.entity.test;

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

    @Column(nullable = false)
    private Integer score;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(nullable = false)
    private Double percentage;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        if (this.submittedAt == null) {
            this.submittedAt = LocalDateTime.now();
        }
    }
}
