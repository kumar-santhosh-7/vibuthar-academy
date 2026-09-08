package com.academy.project.dto.course;

import com.academy.project.enums.CourseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateCourseRequest {

    @NotBlank(message = "Course title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    private Integer durationHours;

    private BigDecimal price;

    private CourseStatus status;
}
