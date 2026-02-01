package com.subscriptiontracker.service;

import com.subscriptiontracker.constant.DomainConstants;
import com.subscriptiontracker.constant.ErrorMessages;
import com.subscriptiontracker.dto.request.CreatePaymentRequest;
import com.subscriptiontracker.dto.request.UpdatePaymentRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.PaymentRecordResponse;
import com.subscriptiontracker.entity.PaymentRecord;
import com.subscriptiontracker.entity.Subscription;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.event.PaymentRecordCreatedEvent;
import com.subscriptiontracker.event.PaymentRecordPaidEvent;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.PaymentRecordRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling payment record management operations.
 *
 * <p>Manages the creation, retrieval, update, and deletion of payment records
 * for subscriptions. Each payment record captures a single payment made for
 * a subscription, including currency conversion to the user's base currency.</p>
 *
 * <p>Payment records are essential for spending analytics and historical
 * tracking of subscription costs over time.</p>
 *
 * @author Generated
 * @since 1.0
 * @see PaymentRecord
 * @see PaymentRecordRepository
 * @see FxRateService
 */
@Service
@RequiredArgsConstructor
public class PaymentRecordService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final FxRateService fxRateService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Retrieves paginated payment records for a specific subscription.
     *
     * @param userId         the ID of the user who owns the subscription
     * @param subscriptionId the ID of the subscription
     * @param page           page number (1-based)
     * @param limit          number of items per page
     * @return paginated response containing payment records sorted by date descending
     * @throws ResourceNotFoundException if the subscription is not found
     */
    public PaginatedResponse<PaymentRecordResponse> getPaymentsForSubscription(
            Long userId, Long subscriptionId, int page, int limit) {

        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_SUBSCRIPTION, DomainConstants.FIELD_ID, subscriptionId));

        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by("paymentDate").descending());
        Page<PaymentRecord> paymentPage = paymentRecordRepository.findBySubscriptionIdOrderByPaymentDateDesc(
                subscriptionId, pageRequest);

        List<PaymentRecordResponse> payments = paymentPage.getContent().stream()
                .map(PaymentRecordResponse::fromEntity)
                .collect(Collectors.toList());

        return PaginatedResponse.of(payments, page, limit, paymentPage.getTotalElements());
    }

    /**
     * Creates a new payment record for a subscription.
     *
     * <p>If no exchange rate is provided, automatically fetches the rate
     * for the payment date from the FX rate service. The amount is
     * converted to the user's base currency using the exchange rate.</p>
     *
     * @param userId  the ID of the user who owns the subscription
     * @param request the payment creation request
     * @return the created payment record response
     * @throws ResourceNotFoundException if the subscription or user is not found
     */
    @Transactional
    public PaymentRecordResponse createPayment(Long userId, CreatePaymentRequest request) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(request.getSubscriptionId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_SUBSCRIPTION, DomainConstants.FIELD_ID, request.getSubscriptionId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_USER, DomainConstants.FIELD_ID, userId));

        BigDecimal fxRate = request.getFxRateToBase();
        if (fxRate == null) {
            fxRate = fxRateService.getRate(
                    request.getCurrencyCode(),
                    user.getBaseCurrencyCode(),
                    request.getPaymentDate()
            );
        }

        BigDecimal amountInBaseCurrency = request.getAmount()
                .multiply(fxRate)
                .setScale(2, RoundingMode.HALF_UP);

        PaymentRecord payment = PaymentRecord.builder()
                .subscription(subscription)
                .amount(request.getAmount())
                .currencyCode(request.getCurrencyCode())
                .paymentDate(request.getPaymentDate())
                .fxRateToBase(fxRate)
                .amountInBaseCurrency(amountInBaseCurrency)
                .build();

        payment = paymentRecordRepository.save(payment);

        eventPublisher.publishEvent(new PaymentRecordCreatedEvent(
                payment.getId(),
                subscription.getId(),
                userId,
                subscription.getService().getName(),
                payment.getAmount(),
                payment.getCurrencyCode(),
                payment.getAmountInBaseCurrency(),
                payment.getPaymentDate()));

        return PaymentRecordResponse.fromEntity(payment);
    }

    /**
     * Updates an existing payment record.
     *
     * <p>Only non-null fields in the request are updated. If the amount,
     * currency, or exchange rate changes, the base currency amount is
     * automatically recalculated.</p>
     *
     * @param userId    the ID of the user who owns the payment record
     * @param paymentId the ID of the payment record to update
     * @param request   the update request with fields to modify
     * @return the updated payment record response
     * @throws ResourceNotFoundException if the payment record or user is not found
     */
    @Transactional
    public PaymentRecordResponse updatePayment(Long userId, Long paymentId, UpdatePaymentRequest request) {
        PaymentRecord payment = paymentRecordRepository.findByIdAndSubscriptionUserId(paymentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_PAYMENT_RECORD, DomainConstants.FIELD_ID, paymentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_USER, DomainConstants.FIELD_ID, userId));

        boolean recalculateBase = false;

        if (request.getAmount() != null) {
            payment.setAmount(request.getAmount());
            recalculateBase = true;
        }

        if (request.getCurrencyCode() != null) {
            payment.setCurrencyCode(request.getCurrencyCode());
            recalculateBase = true;
        }

        if (request.getPaymentDate() != null) {
            payment.setPaymentDate(request.getPaymentDate());
        }

        if (request.getFxRateToBase() != null) {
            payment.setFxRateToBase(request.getFxRateToBase());
            recalculateBase = true;
        }

        if (recalculateBase) {
            BigDecimal amountInBaseCurrency = payment.getAmount()
                    .multiply(payment.getFxRateToBase())
                    .setScale(2, RoundingMode.HALF_UP);
            payment.setAmountInBaseCurrency(amountInBaseCurrency);
        }

        payment = paymentRecordRepository.save(payment);
        return PaymentRecordResponse.fromEntity(payment);
    }

    /**
     * Marks a pending payment as paid.
     *
     * <p>Transitions a payment from pending to paid status and records
     * the actual date when the payment was made. Publishes a
     * {@link PaymentRecordPaidEvent} upon successful transition.</p>
     *
     * @param userId    the ID of the user who owns the payment record
     * @param paymentId the ID of the payment record to mark as paid
     * @param paidDate  the actual date when the payment was made
     * @return the updated payment record response
     * @throws ResourceNotFoundException if the payment record is not found
     * @throws com.subscriptiontracker.exception.BadRequestException if the payment is not in pending status
     */
    @Transactional
    public PaymentRecordResponse markAsPaid(Long userId, Long paymentId, LocalDate paidDate) {
        PaymentRecord payment = paymentRecordRepository.findByIdAndSubscriptionUserId(paymentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_PAYMENT_RECORD, DomainConstants.FIELD_ID, paymentId));

        payment.markAsPaid(paidDate);
        payment = paymentRecordRepository.save(payment);

        Subscription subscription = payment.getSubscription();
        eventPublisher.publishEvent(new PaymentRecordPaidEvent(
                payment.getId(),
                subscription.getId(),
                userId,
                subscription.getService().getName(),
                payment.getAmount(),
                payment.getCurrencyCode(),
                paidDate));

        return PaymentRecordResponse.fromEntity(payment);
    }

    /**
     * Deletes a payment record.
     *
     * @param userId    the ID of the user who owns the payment record
     * @param paymentId the ID of the payment record to delete
     * @throws ResourceNotFoundException if the payment record is not found
     */
    @Transactional
    public void deletePayment(Long userId, Long paymentId) {
        PaymentRecord payment = paymentRecordRepository.findByIdAndSubscriptionUserId(paymentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_PAYMENT_RECORD, DomainConstants.FIELD_ID, paymentId));

        paymentRecordRepository.delete(payment);
    }
}
