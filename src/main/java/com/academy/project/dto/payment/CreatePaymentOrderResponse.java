package com.academy.project.dto.payment;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CreatePaymentOrderResponse {

    private Long paymentId;
    private String keyId;
    private String orderId;
    private BigDecimal amount;
    private Long amountPaise;
    private String currency;
    private String courseId;
    private String courseTitle;
    private String studentName;
    private String studentEmail;
    private String studentPhone;
    private String receipt;
}
