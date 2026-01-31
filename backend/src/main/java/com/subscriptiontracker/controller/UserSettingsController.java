package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.request.ChangePasswordRequest;
import com.subscriptiontracker.dto.request.DeleteAccountRequest;
import com.subscriptiontracker.dto.request.UpdateUserSettingsRequest;
import com.subscriptiontracker.dto.response.UserSettingsResponse;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for user settings and account management.
 *
 * <p>Handles user preferences, password changes, and account deletion.</p>
 *
 * @author Generated
 * @since 1.0
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User Settings", description = "User preferences and account management")
public class UserSettingsController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    /**
     * Retrieves the current user's settings.
     *
     * @param userDetails the authenticated user details
     * @return the user settings
     */
    @Operation(
            summary = "Get user settings",
            description = "Retrieves the current user's preferences including default currency."
    )
    @ApiResponse(responseCode = "200", description = "Settings retrieved successfully")
    @GetMapping("/settings")
    public ResponseEntity<UserSettingsResponse> getSettings(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        UserSettingsResponse response = userService.getSettings(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the current user's settings.
     *
     * @param userDetails the authenticated user details
     * @param request the settings update request
     * @return the updated user settings
     */
    @Operation(
            summary = "Update user settings",
            description = "Updates the current user's preferences including default currency."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Settings updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid settings data")
    })
    @PutMapping("/settings")
    public ResponseEntity<UserSettingsResponse> updateSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateUserSettingsRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        UserSettingsResponse response = userService.updateSettings(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Changes the current user's password.
     *
     * @param userDetails the authenticated user details
     * @param request the password change request
     * @return success message
     */
    @Operation(
            summary = "Change password",
            description = "Changes the current user's password. Requires the current password for verification."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid password or password requirements not met"),
            @ApiResponse(responseCode = "401", description = "Current password is incorrect")
    })
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        userService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    /**
     * Permanently deletes the current user's account.
     *
     * @param userDetails the authenticated user details
     * @param request the account deletion request
     * @return empty response on successful deletion
     */
    @Operation(
            summary = "Delete account",
            description = "Permanently deletes the user's account and all associated data. This action cannot be undone."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Password verification failed")
    })
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DeleteAccountRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        userService.deleteAccount(userId, request);
        return ResponseEntity.noContent().build();
    }
}
