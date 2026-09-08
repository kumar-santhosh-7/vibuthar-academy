package com.academy.project.controller.admin;

import com.academy.project.dto.subscription.CreateSubscriptionRequest;
import com.academy.project.dto.subscription.PaidAmountResponse;
import com.academy.project.dto.subscription.SubscriptionResponse;
import com.academy.project.dto.subscription.SubscriptionStatsResponse;
import com.academy.project.dto.subscription.UpdateSubscriptionPaymentRequest;
import com.academy.project.dto.response.ApiResponse;
import com.academy.project.service.subscription.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/subscriptions")
@RequiredArgsConstructor
@CrossOrigin
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<ApiResponse<SubscriptionStatsResponse>> getSubscriptionStats() {
        SubscriptionStatsResponse response = subscriptionService.getSubscriptionStats();
        return ResponseEntity.ok(ApiResponse.ok("Subscription stats fetched successfully", response));
    }

    @GetMapping("/paid-amount")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaidAmountResponse>> getTotalPaidAmount(
            @RequestParam(required = false) String courseId) {
        PaidAmountResponse response = subscriptionService.getTotalPaidAmount(courseId);
        return ResponseEntity.ok(ApiResponse.ok("Total paid amount fetched successfully", response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Subscription created successfully", response));
    }

    @PatchMapping("/{subscriptionId}/payment")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> updateSubscriptionPayment(
            @PathVariable Long subscriptionId,
            @Valid @RequestBody UpdateSubscriptionPaymentRequest request) {
        SubscriptionResponse response = subscriptionService.updateSubscriptionPayment(subscriptionId, request);
        return ResponseEntity.ok(ApiResponse.ok("Subscription payment updated successfully", response));
    }
}
