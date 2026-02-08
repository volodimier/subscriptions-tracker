package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.request.CancelSubscriptionRequest;
import com.subscriptiontracker.dto.request.CreateSubscriptionRequest;
import com.subscriptiontracker.dto.request.ReactivateSubscriptionRequest;
import com.subscriptiontracker.dto.request.UpdateSubscriptionRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.SubscriptionDetailResponse;
import com.subscriptiontracker.dto.response.SubscriptionHistoryResponse;
import com.subscriptiontracker.dto.response.SubscriptionResponse;
import com.subscriptiontracker.entity.SubscriptionStatus;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.SubscriptionService;
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

import java.util.List;

/**
 * REST controller for subscription management operations.
 *
 * <p>Handles CRUD operations for subscriptions including creating, updating,
 * cancelling, reactivating, and deleting subscriptions.</p>
 *
 * @author Generated
 * @since 1.0
 */
@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Subscription CRUD and management operations")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUserService currentUserService;

    /**
     * Retrieves a paginated list of subscriptions for the current user.
     *
     * @param userDetails the authenticated user details
     * @param status optional filter by subscription status
     * @param category optional filter by category
     * @param search optional search term
     * @param sort field to sort by
     * @param order sort order (asc/desc)
     * @param page page number (1-based)
     * @param limit number of items per page
     * @return paginated list of subscriptions
     */
    @Operation(
            summary = "List subscriptions",
            description = "Retrieves a paginated list of subscriptions for the authenticated user. "
                    + "Supports filtering by status, category, and search term."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscriptions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping
    public ResponseEntity<PaginatedResponse<SubscriptionResponse>> getSubscriptions(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Filter by status (ACTIVE, CANCELLED, PAUSED, or 'all')")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by category name")
            @RequestParam(required = false) String category,
            @Parameter(description = "Search term for service name")
            @RequestParam(required = false) String search,
            @Parameter(description = "Sort field (nextBillingDate, amount, createdAt)")
            @RequestParam(defaultValue = "nextBillingDate") String sort,
            @Parameter(description = "Sort order (asc, desc)")
            @RequestParam(defaultValue = "asc") String order,
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page")
            @RequestParam(defaultValue = "20") int limit
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        SubscriptionStatus statusEnum = null;
        if (status != null && !status.equals("all")) {
            statusEnum = SubscriptionStatus.valueOf(status);
        }

        PaginatedResponse<SubscriptionResponse> response = subscriptionService.getSubscriptions(
                userId, statusEnum, category, search, sort, order, page, limit
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves detailed information about a specific subscription.
     *
     * @param userDetails the authenticated user details
     * @param id the subscription ID
     * @return the subscription details
     */
    @Operation(
            summary = "Get subscription details",
            description = "Retrieves detailed information about a specific subscription including payment history."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionDetailResponse> getSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Subscription ID") @PathVariable Long id
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        SubscriptionDetailResponse response = subscriptionService.getSubscriptionDetail(userId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new subscription for the current user.
     *
     * @param userDetails the authenticated user details
     * @param request the subscription creation request
     * @return the created subscription
     */
    @Operation(
            summary = "Create subscription",
            description = "Creates a new subscription for the authenticated user with the specified service, "
                    + "amount, currency, and billing cycle."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Subscription created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateSubscriptionRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        SubscriptionResponse response = subscriptionService.createSubscription(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing subscription.
     *
     * @param userDetails the authenticated user details
     * @param id the subscription ID
     * @param request the update request
     * @return the updated subscription
     */
    @Operation(
            summary = "Update subscription",
            description = "Updates an existing subscription's details including amount, billing cycle, and notes."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Subscription ID") @PathVariable Long id,
            @Valid @RequestBody UpdateSubscriptionRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        SubscriptionResponse response = subscriptionService.updateSubscription(userId, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels a subscription.
     *
     * @param userDetails the authenticated user details
     * @param id the subscription ID
     * @param request the cancellation request
     * @return the cancelled subscription
     */
    @Operation(
            summary = "Cancel subscription",
            description = "Cancels an active subscription. The subscription can optionally be reactivated later."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Subscription already cancelled"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Subscription ID") @PathVariable Long id,
            @Valid @RequestBody CancelSubscriptionRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        SubscriptionResponse response = subscriptionService.cancelSubscription(userId, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reactivates a cancelled subscription.
     *
     * @param userDetails the authenticated user details
     * @param id the subscription ID
     * @param request the reactivation request
     * @return the reactivated subscription
     */
    @Operation(
            summary = "Reactivate subscription",
            description = "Reactivates a previously cancelled subscription with a new billing date."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription reactivated successfully"),
            @ApiResponse(responseCode = "400", description = "Subscription is not cancelled"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<SubscriptionResponse> reactivateSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Subscription ID") @PathVariable Long id,
            @Valid @RequestBody ReactivateSubscriptionRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        SubscriptionResponse response = subscriptionService.reactivateSubscription(userId, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Permanently deletes a subscription.
     *
     * @param userDetails the authenticated user details
     * @param id the subscription ID
     * @return empty response on successful deletion
     */
    @Operation(
            summary = "Delete subscription",
            description = "Permanently deletes a subscription and all associated payment records."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Subscription deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Subscription ID") @PathVariable Long id
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        subscriptionService.deleteSubscription(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the change history for a specific subscription.
     *
     * <p>Returns a timeline of historical snapshots showing how the subscription's
     * tracked fields changed over time, ordered by most recent first.</p>
     *
     * @param userDetails the authenticated user details
     * @param id the subscription ID
     * @return list of history entries
     */
    @Operation(
            summary = "Get subscription history",
            description = "Retrieves the change history timeline for a specific subscription, "
                    + "showing how tracked fields changed over time."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @GetMapping("/{id}/history")
    public ResponseEntity<List<SubscriptionHistoryResponse>> getSubscriptionHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Subscription ID") @PathVariable Long id
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        List<SubscriptionHistoryResponse> history = subscriptionService.getSubscriptionHistory(userId, id);
        return ResponseEntity.ok(history);
    }

    /**
     * Retrieves all unique categories from the user's subscriptions.
     *
     * @param userDetails the authenticated user details
     * @return list of category names
     */
    @Operation(
            summary = "Get subscription categories",
            description = "Retrieves a list of all unique categories from the user's subscriptions."
    )
    @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        List<String> categories = subscriptionService.getCategories(userId);
        return ResponseEntity.ok(categories);
    }
}
