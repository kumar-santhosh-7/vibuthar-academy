package com.academy.project.controller.payment;

import com.academy.project.dto.payment.CreatePaymentOrderRequest;
import com.academy.project.dto.payment.CreatePaymentOrderResponse;
import com.academy.project.dto.payment.PaymentStatusResponse;
import com.academy.project.dto.payment.VerifyPaymentRequest;
import com.academy.project.dto.response.ApiResponse;
import com.academy.project.dto.subscription.SubscriptionResponse;
import com.academy.project.service.payment.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin
public class PaymentController {

    private final PaymentService paymentService;

    /** Student: create Razorpay order for a course (amount taken from course price on server). */
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<CreatePaymentOrderResponse>> createOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request) {
        CreatePaymentOrderResponse response = paymentService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Payment order created successfully", response));
    }

    /**
     * Student: verify checkout success signature and activate subscription.
     * Prefer webhook as source of truth; this gives faster UX after Checkout.
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {
        SubscriptionResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(ApiResponse.ok("Payment verified and subscription activated", response));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getStatus(@PathVariable String orderId) {
        PaymentStatusResponse response = paymentService.getStatus(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Payment status fetched successfully", response));
    }

    /**
     * Razorpay server webhook. Must remain public; authenticity is via X-Razorpay-Signature.
     * Configure URL in Razorpay dashboard: POST /api/payments/webhook
     * Enable events: payment.captured (recommended), order.paid
     */
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok(ApiResponse.ok("Webhook processed", null));
    }
}
