package com.academy.project.dto.subscription;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriptionStatsResponse {

    private long subscribedMemberCount;
    private long unsubscribedMemberCount;
    private long courseCount;
}
