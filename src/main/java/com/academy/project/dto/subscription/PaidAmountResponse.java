package com.academy.project.dto.subscription;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PaidAmountResponse {

    private BigDecimal totalPaidAmount;
}
