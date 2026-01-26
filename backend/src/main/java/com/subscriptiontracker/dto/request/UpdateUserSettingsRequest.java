package com.subscriptiontracker.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserSettingsRequest {

    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String baseCurrency;
}
