package com.subscriptiontracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for permanently deleting a user account.
 *
 * <p>Requires password verification and explicit confirmation to prevent
 * accidental deletions. This action cannot be undone.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequest {

    /** The user's current password for verification. */
    @NotBlank(message = "Password is required")
    private String password;

    /** Must be exactly "DELETE" to confirm the action. */
    @NotBlank(message = "Confirmation is required")
    private String confirmation;
}
