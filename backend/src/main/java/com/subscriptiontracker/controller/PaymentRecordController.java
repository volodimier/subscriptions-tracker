package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.request.CreatePaymentRequest;
import com.subscriptiontracker.dto.request.UpdatePaymentRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.PaymentRecordResponse;
import com.subscriptiontracker.entity.TriggerType;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.UserRepository;
import com.subscriptiontracker.service.FxRateService;
import com.subscriptiontracker.service.JobRunService;
import com.subscriptiontracker.service.PaymentRecordService;
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

@RestController
@RequiredArgsConstructor
public class PaymentRecordController {

    private final PaymentRecordService paymentRecordService;
    private final FxRateService fxRateService;
    private final JobRunService jobRunService;
    private final UserRepository userRepository;

    @GetMapping("/subscriptions/{subscriptionId}/payments")
    public ResponseEntity<PaginatedResponse<PaymentRecordResponse>> getPaymentsForSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long subscriptionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Long userId = getUserId(userDetails);
        PaginatedResponse<PaymentRecordResponse> response = paymentRecordService.getPaymentsForSubscription(
                userId, subscriptionId, page, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentRecordResponse> createPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        Long userId = getUserId(userDetails);
        PaymentRecordResponse response = paymentRecordService.createPayment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/payments/{id}")
    public ResponseEntity<PaymentRecordResponse> updatePayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePaymentRequest request
    ) {
        Long userId = getUserId(userDetails);
        PaymentRecordResponse response = paymentRecordService.updatePayment(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/payments/{id}")
    public ResponseEntity<Void> deletePayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        Long userId = getUserId(userDetails);
        paymentRecordService.deletePayment(userId, id);
        return ResponseEntity.noContent().build();
    }

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

    private Long getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()));
        return user.getId();
    }
}
