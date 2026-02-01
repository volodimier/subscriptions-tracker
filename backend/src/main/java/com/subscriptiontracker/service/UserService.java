package com.subscriptiontracker.service;

import com.subscriptiontracker.constant.DomainConstants;
import com.subscriptiontracker.constant.ErrorMessages;
import com.subscriptiontracker.dto.request.ChangePasswordRequest;
import com.subscriptiontracker.dto.request.DeleteAccountRequest;
import com.subscriptiontracker.dto.request.UpdateUserSettingsRequest;
import com.subscriptiontracker.dto.response.UserSettingsResponse;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service handling user account and settings management.
 *
 * <p>Provides functionality for managing user preferences, changing
 * passwords, and deleting accounts. Includes security measures
 * such as password verification for sensitive operations.</p>
 *
 * @author Generated
 * @since 1.0
 * @see User
 * @see UserSettingsResponse
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FxRateService fxRateService;
    private final JobRunService jobRunService;

    /**
     * Retrieves user settings including preferences and FX rate information.
     *
     * @param userId the ID of the user
     * @return user settings response with current FX rates
     * @throws ResourceNotFoundException if the user is not found
     */
    public UserSettingsResponse getSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_USER, DomainConstants.FIELD_ID, userId));

        Map<String, BigDecimal> currentRates = fxRateService.getCurrentRates(user.getBaseCurrencyCode());
        LocalDateTime lastUpdate = jobRunService.getLatestSuccessfulFxRateRefreshDateTime()
                .orElse(null);

        return UserSettingsResponse.builder()
                .email(user.getEmail())
                .baseCurrency(user.getBaseCurrencyCode())
                .fxRatesLastUpdated(lastUpdate)
                .currentFxRates(currentRates)
                .build();
    }

    /**
     * Updates user settings.
     *
     * @param userId  the ID of the user
     * @param request the settings update request
     * @return updated user settings response
     * @throws ResourceNotFoundException if the user is not found
     */
    @Transactional
    public UserSettingsResponse updateSettings(Long userId, UpdateUserSettingsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_USER, DomainConstants.FIELD_ID, userId));

        if (request.getBaseCurrency() != null) {
            user.setBaseCurrencyCode(request.getBaseCurrency().toUpperCase());
        }

        user = userRepository.save(user);
        return getSettings(userId);
    }

    /**
     * Changes the user's password.
     *
     * <p>Requires the current password for verification before
     * updating to the new password.</p>
     *
     * @param userId  the ID of the user
     * @param request the password change request containing current and new passwords
     * @throws ResourceNotFoundException if the user is not found
     * @throws BadRequestException       if the current password is incorrect
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_USER, DomainConstants.FIELD_ID, userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException(ErrorMessages.CURRENT_PASSWORD_INCORRECT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Permanently deletes a user account and all associated data.
     *
     * <p>Requires password verification and explicit confirmation (typing "DELETE")
     * to prevent accidental deletions. This action cannot be undone.</p>
     *
     * @param userId  the ID of the user
     * @param request the deletion request with password and confirmation
     * @throws ResourceNotFoundException if the user is not found
     * @throws BadRequestException       if the password is incorrect or confirmation is invalid
     */
    @Transactional
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_USER, DomainConstants.FIELD_ID, userId));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException(ErrorMessages.PASSWORD_INCORRECT);
        }

        if (!DomainConstants.DELETE_CONFIRMATION_TEXT.equals(request.getConfirmation())) {
            throw new BadRequestException(ErrorMessages.DELETE_CONFIRMATION_REQUIRED);
        }

        userRepository.delete(user);
    }
}
