package com.academy.project.service.payment;

import com.academy.project.dto.payment.CreatePaymentOrderRequest;
import com.academy.project.dto.payment.CreatePaymentOrderResponse;
import com.academy.project.dto.payment.PaymentStatusResponse;
import com.academy.project.dto.payment.VerifyPaymentRequest;
import com.academy.project.dto.subscription.SubscriptionResponse;

public interface PaymentService {

    CreatePaymentOrderResponse createOrder(CreatePaymentOrderRequest request);

    SubscriptionResponse verifyPayment(VerifyPaymentRequest request);

    PaymentStatusResponse getStatus(String orderId);

    void handleWebhook(String payload, String signature);
}
