package com.academy.project.service.subscription;

import com.academy.project.dto.subscription.CreateSubscriptionRequest;
import com.academy.project.dto.subscription.PaidAmountResponse;
import com.academy.project.dto.subscription.SubscriptionResponse;
import com.academy.project.dto.subscription.SubscriptionStatsResponse;
import com.academy.project.dto.subscription.UpdateSubscriptionPaymentRequest;

public interface SubscriptionService {

    SubscriptionResponse createSubscription(CreateSubscriptionRequest request);

    SubscriptionResponse updateSubscriptionPayment(Long subscriptionId, UpdateSubscriptionPaymentRequest request);

    SubscriptionStatsResponse getSubscriptionStats();

    PaidAmountResponse getTotalPaidAmount(String courseId);
}
