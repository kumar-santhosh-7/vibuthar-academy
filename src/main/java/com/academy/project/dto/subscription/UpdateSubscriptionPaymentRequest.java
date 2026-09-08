package com.academy.project.dto.subscription;

import com.academy.project.enums.PaymentStatus;
import com.academy.project.enums.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateSubscriptionPaymentRequest {

    @NotBlank(message = "Student id is required")
    private String studentId;

    private PaymentType paymentType;

    private PaymentStatus paymentStatus;

    @DecimalMin(value = "0.00", message = "Amount cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;
}
