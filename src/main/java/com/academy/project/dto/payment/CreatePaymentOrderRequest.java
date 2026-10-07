package com.academy.project.dto.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentOrderRequest {

    @NotBlank(message = "Course id is required")
    private String courseId;
}
