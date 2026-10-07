package com.academy.project.dto.payment;

import com.academy.project.enums.PaymentOrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentStatusResponse {

    private Long paymentId;
    private String courseId;
    private String orderId;
    private String razorpayPaymentId;
    private PaymentOrderStatus status;
    private BigDecimal amount;
    private String currency;
    private Long subscriptionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
