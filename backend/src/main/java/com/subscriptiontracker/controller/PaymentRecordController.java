package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.request.CreatePaymentRequest;
import com.subscriptiontracker.dto.request.UpdatePaymentRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.PaymentRecordResponse;
import com.subscriptiontracker.entity.TriggerType;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.FxRateService;
import com.subscriptiontracker.service.JobRunService;
import com.subscriptiontracker.service.PaymentRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST controller for payment records and FX rate management.
 *
 * <p>Handles CRUD operations for payment records and foreign exchange rate refreshes.</p>
 *
 * @author Generated
 * @since 1.0
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment records and FX rate management")
public class PaymentRecordController {

    private final PaymentRecordService paymentRecordService;
    private final FxRateService fxRateService;
    private final JobRunService jobRunService;
    private final CurrentUserService currentUserService;

    /**
     * Retrieves payment records for a specific subscription.
     *
     * @param userDetails the authenticated user details
     * @param subscriptionId the subscription ID
     * @param page page number (1-based)
     * @param limit number of items per page
     * @return paginated list of payment records
     */
    @Operation(
            summary = "List payments for subscription",
            description = "Retrieves a paginated list of payment records for a specific subscription."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payments retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @GetMapping("/subscriptions/{subscriptionId}/payments")
    public ResponseEntity<PaginatedResponse<PaymentRecordResponse>> getPaymentsForSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Subscription ID") @PathVariable Long subscriptionId,
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "20") int limit
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        PaginatedResponse<PaymentRecordResponse> response = paymentRecordService.getPaymentsForSubscription(
                userId, subscriptionId, page, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new payment record.
     *
     * @param userDetails the authenticated user details
     * @param request the payment creation request
     * @return the created payment record
     */
    @Operation(
            summary = "Create payment record",
            description = "Creates a new manual payment record for a subscription."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @PostMapping("/payments")
    public ResponseEntity<PaymentRecordResponse> createPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        PaymentRecordResponse response = paymentRecordService.createPayment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing payment record.
     *
     * @param userDetails the authenticated user details
     * @param id the payment record ID
     * @param request the update request
     * @return the updated payment record
     */
    @Operation(
            summary = "Update payment record",
            description = "Updates an existing payment record's details."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @PutMapping("/payments/{id}")
    public ResponseEntity<PaymentRecordResponse> updatePayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Payment record ID") @PathVariable Long id,
            @Valid @RequestBody UpdatePaymentRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        PaymentRecordResponse response = paymentRecordService.updatePayment(userId, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a payment record.
     *
     * @param userDetails the authenticated user details
     * @param id the payment record ID
     * @return empty response on successful deletion
     */
    @Operation(
            summary = "Delete payment record",
            description = "Permanently deletes a payment record."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Payment deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @DeleteMapping("/payments/{id}")
    public ResponseEntity<Void> deletePayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Payment record ID") @PathVariable Long id
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        paymentRecordService.deletePayment(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Manually refreshes foreign exchange rates.
     *
     * @param userDetails the authenticated user details
     * @return the refresh result with updated rates
     */
    @Operation(
            summary = "Refresh FX rates",
            description = "Manually triggers a refresh of foreign exchange rates from the external API."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "FX rates refreshed successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch FX rates from external API")
    })
    @PostMapping("/fx-rates/refresh")
    public ResponseEntity<Map<String, Object>> refreshFxRates(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        LocalDateTime startTime = LocalDateTime.now();
        try {
            Map<String, BigDecimal> rates = fxRateService.refreshRates();
            LocalDateTime finishTime = LocalDateTime.now();
            jobRunService.recordSuccess(JobRunService.JOB_FX_RATE_REFRESH, startTime, finishTime, TriggerType.MANUAL);
            return ResponseEntity.ok(Map.of(
                    "message", "FX rates refreshed successfully",
                    "updatedAt", finishTime.toString(),
                    "rates", rates
            ));
        } catch (Exception e) {
            LocalDateTime finishTime = LocalDateTime.now();
            jobRunService.recordFailure(JobRunService.JOB_FX_RATE_REFRESH, startTime, finishTime, TriggerType.MANUAL, e.getMessage());
            throw e;
        }
    }
}
