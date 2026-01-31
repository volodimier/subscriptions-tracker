package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.request.CancelSubscriptionRequest;
import com.subscriptiontracker.dto.request.CreateSubscriptionRequest;
import com.subscriptiontracker.dto.request.ReactivateSubscriptionRequest;
import com.subscriptiontracker.dto.request.UpdateSubscriptionRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.SubscriptionDetailResponse;
import com.subscriptiontracker.dto.response.SubscriptionResponse;
import com.subscriptiontracker.entity.SubscriptionStatus;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<SubscriptionResponse>> getSubscriptions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "nextBillingDate") String sort,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(defaultValue = "1") int page,
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

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionDetailResponse> getSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        SubscriptionDetailResponse response = subscriptionService.getSubscriptionDetail(userId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateSubscriptionRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        SubscriptionResponse response = subscriptionService.createSubscription(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubscriptionRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        SubscriptionResponse response = subscriptionService.updateSubscription(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody CancelSubscriptionRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        SubscriptionResponse response = subscriptionService.cancelSubscription(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<SubscriptionResponse> reactivateSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ReactivateSubscriptionRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        SubscriptionResponse response = subscriptionService.reactivateSubscription(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        subscriptionService.deleteSubscription(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        List<String> categories = subscriptionService.getCategories(userId);
        return ResponseEntity.ok(categories);
    }
}
