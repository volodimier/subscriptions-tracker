package com.subscriptiontracker.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user settings.
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserSettingsRequest {

    /**
     * The user's preferred base currency (ISO 4217 code).
     * All spending analytics will be converted to this currency.
     */
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String baseCurrency;
}
