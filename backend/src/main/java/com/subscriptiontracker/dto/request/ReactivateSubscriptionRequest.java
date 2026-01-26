package com.subscriptiontracker.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactivateSubscriptionRequest {

    @NotNull(message = "Next billing date is required")
    private LocalDate nextBillingDate;
}
