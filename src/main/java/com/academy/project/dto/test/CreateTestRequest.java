package com.academy.project.dto.test;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTestRequest {

    @NotBlank(message = "Test title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    /** Time limit in minutes; optional. */
    private Integer durationMinutes;

    /** Total marks for the test; optional (defaults to question count). */
    private Integer totalMarks;

    /** Minimum marks to pass; optional. */
    private Integer cutOff;
}
