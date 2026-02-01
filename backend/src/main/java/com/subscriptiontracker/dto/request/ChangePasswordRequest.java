package com.subscriptiontracker.dto.request;

import com.subscriptiontracker.validation.PasswordMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for changing the user's password.
 *
 * <p>The current password is required for verification. The new password
 * must meet complexity requirements and match the confirmation.</p>
 *
 * @author Generated
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@PasswordMatch
public class ChangePasswordRequest {

    /** The user's current password for verification. */
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    /** The new password (min 8 chars, must contain uppercase and number). */
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
            message = "New password must contain at least one uppercase letter and one number"
    )
    private String newPassword;

    /** Confirmation of the new password (must match newPassword). */
    @NotBlank(message = "Password confirmation is required")
    private String confirmNewPassword;
}
