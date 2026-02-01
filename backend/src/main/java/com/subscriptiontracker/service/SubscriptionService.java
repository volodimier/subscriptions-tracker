package com.subscriptiontracker.service;

import com.subscriptiontracker.constant.DomainConstants;
import com.subscriptiontracker.constant.ErrorMessages;
import com.subscriptiontracker.dto.request.CancelSubscriptionRequest;
import com.subscriptiontracker.dto.request.CreateSubscriptionRequest;
import com.subscriptiontracker.dto.request.ReactivateSubscriptionRequest;
import com.subscriptiontracker.dto.request.UpdateSubscriptionRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.SubscriptionDetailResponse;
import com.subscriptiontracker.dto.response.SubscriptionResponse;
import com.subscriptiontracker.entity.*;
import com.subscriptiontracker.event.SubscriptionCancelledEvent;
import com.subscriptiontracker.event.SubscriptionCreatedEvent;
import com.subscriptiontracker.event.SubscriptionReactivatedEvent;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.domain.valueobject.BillingPeriod;
import com.subscriptiontracker.repository.PaymentRecordRepository;
import com.subscriptiontracker.repository.ServiceRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling subscription management operations.
 *
 * <p>Provides comprehensive CRUD functionality for subscriptions including:</p>
 * <ul>
 *   <li>Creating new subscriptions with validation</li>
 *   <li>Listing subscriptions with filtering, sorting, and pagination</li>
 *   <li>Updating subscription details</li>
 *   <li>Cancelling and reactivating subscriptions</li>
 *   <li>Calculating subscription statistics</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see Subscription
 * @see SubscriptionRepository
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Retrieves a paginated list of subscriptions for a user with optional filters.
     *
     * @param userId   the ID of the user whose subscriptions to retrieve
     * @param status   optional filter by subscription status
     * @param category optional filter by service category
     * @param search   optional search term for service name
     * @param sortBy   field to sort by (amount, name, or nextBillingDate)
     * @param order    sort order (asc or desc)
     * @param page     page number (1-based)
     * @param limit    number of items per page
     * @return paginated response containing subscription data
     */
    public PaginatedResponse<SubscriptionResponse> getSubscriptions(
            Long userId,
            SubscriptionStatus status,
            String category,
            String search,
            String sortBy,
            String order,
            int page,
            int limit
    ) {
        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortField = mapSortField(sortBy);
        Sort sort = JpaSort.unsafe(direction, "(" + sortField + ")");
        PageRequest pageRequest = PageRequest.of(page - 1, limit, sort);

        Page<Subscription> subscriptionPage = subscriptionRepository.findByUserIdWithFilters(
                userId, status != null ? status.name() : null, category, search, pageRequest
        );

        List<SubscriptionResponse> subscriptions = subscriptionPage.getContent().stream()
                .map(SubscriptionResponse::fromEntity)
                .collect(Collectors.toList());

        return PaginatedResponse.of(subscriptions, page, limit, subscriptionPage.getTotalElements());
    }

    private String mapSortField(String sortBy) {
        // Native query uses actual column names
        return switch (sortBy) {
            case "amount" -> "amount";
            case "name" -> "srv.name";
            default -> "next_billing_date";
        };
    }

    /**
     * Retrieves detailed information about a specific subscription.
     *
     * <p>Includes subscription statistics such as total paid, number of payments,
     * and average monthly spending.</p>
     *
     * @param userId         the ID of the user who owns the subscription
     * @param subscriptionId the ID of the subscription to retrieve
     * @return detailed subscription response with statistics
     * @throws ResourceNotFoundException if the subscription is not found
     */
    public SubscriptionDetailResponse getSubscriptionDetail(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_SUBSCRIPTION, DomainConstants.FIELD_ID, subscriptionId));

        SubscriptionDetailResponse.Stats stats = calculateStats(subscription);
        return SubscriptionDetailResponse.fromEntity(subscription, stats);
    }

    private SubscriptionDetailResponse.Stats calculateStats(Subscription subscription) {
        BigDecimal totalPaid = paymentRecordRepository.sumAmountInBaseCurrencyBySubscriptionId(subscription.getId());
        long totalPayments = paymentRecordRepository.countBySubscriptionId(subscription.getId());

        BigDecimal averagePerMonth = BigDecimal.ZERO;
        if (totalPayments > 0) {
            long monthsActive = ChronoUnit.MONTHS.between(subscription.getStartDate(), LocalDate.now());
            if (monthsActive > 0) {
                averagePerMonth = totalPaid.divide(BigDecimal.valueOf(monthsActive), 2, RoundingMode.HALF_UP);
            } else {
                averagePerMonth = totalPaid;
            }
        }

        return SubscriptionDetailResponse.Stats.builder()
                .totalPaid(totalPaid)
                .totalPayments(totalPayments)
                .averagePerMonth(averagePerMonth)
                .activeSince(subscription.getStartDate())
                .build();
    }

    /**
     * Creates a new subscription for a user.
     *
     * <p>Validates that the referenced service exists and belongs to the user.
     * For custom billing cycles, the billingCycleDays field must be provided.</p>
     *
     * <p>The start date is automatically calculated based on the next billing date
     * and billing cycle. Any start date provided in the request is ignored.</p>
     *
     * @param userId  the ID of the user creating the subscription
     * @param request the subscription creation request
     * @return the created subscription response
     * @throws ResourceNotFoundException if the user or service is not found
     * @throws BadRequestException       if custom billing cycle is selected without days specified
     */
    @Transactional
    public SubscriptionResponse createSubscription(Long userId, CreateSubscriptionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_USER, DomainConstants.FIELD_ID, userId));

        Service service = serviceRepository.findByIdAndUserId(request.getServiceId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_SERVICE, DomainConstants.FIELD_ID, request.getServiceId()));

        if (request.getBillingCycle() == BillingCycle.custom && request.getBillingCycleDays() == null) {
            throw new BadRequestException(ErrorMessages.BILLING_CYCLE_DAYS_REQUIRED);
        }

        // Auto-calculate start date from next billing date and billing cycle
        BillingPeriod billingPeriod = BillingPeriod.of(request.getBillingCycle(), request.getBillingCycleDays());
        LocalDate calculatedStartDate = billingPeriod.calculateStartDate(request.getNextBillingDate());

        Subscription subscription = Subscription.builder()
                .user(user)
                .service(service)
                .amount(request.getAmount())
                .currencyCode(request.getCurrencyCode())
                .billingCycle(request.getBillingCycle())
                .billingCycleDays(request.getBillingCycleDays())
                .paymentMethod(request.getPaymentMethod())
                .startDate(calculatedStartDate)
                .nextBillingDate(request.getNextBillingDate())
                .notes(request.getNotes())
                .status(SubscriptionStatus.active)
                .build();

        subscription = subscriptionRepository.save(subscription);

        eventPublisher.publishEvent(new SubscriptionCreatedEvent(
                subscription.getId(),
                userId,
                service.getId(),
                service.getName(),
                subscription.getAmount(),
                subscription.getCurrencyCode()));

        return SubscriptionResponse.fromEntity(subscription);
    }

    /**
     * Updates an existing subscription.
     *
     * <p>Only non-null fields in the request are updated. The subscription
     * must belong to the specified user.</p>
     *
     * <p>If the billing cycle or next billing date is changed, the start date
     * is automatically recalculated to maintain consistency.</p>
     *
     * @param userId         the ID of the user who owns the subscription
     * @param subscriptionId the ID of the subscription to update
     * @param request        the update request with fields to modify
     * @return the updated subscription response
     * @throws ResourceNotFoundException if the subscription or service is not found
     * @throws BadRequestException       if custom billing cycle is selected without days specified
     */
    @Transactional
    public SubscriptionResponse updateSubscription(Long userId, Long subscriptionId, UpdateSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_SUBSCRIPTION, DomainConstants.FIELD_ID, subscriptionId));

        if (request.getServiceId() != null) {
            Service service = serviceRepository.findByIdAndUserId(request.getServiceId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorMessages.RESOURCE_SERVICE, DomainConstants.FIELD_ID, request.getServiceId()));
            subscription.setService(service);
        }

        if (request.getAmount() != null) {
            subscription.setAmount(request.getAmount());
        }

        if (request.getCurrencyCode() != null) {
            subscription.setCurrencyCode(request.getCurrencyCode());
        }

        // Track if billing-related fields changed to trigger start date recalculation
        boolean billingChanged = false;

        if (request.getBillingCycle() != null) {
            subscription.setBillingCycle(request.getBillingCycle());
            if (request.getBillingCycle() == BillingCycle.custom && request.getBillingCycleDays() == null) {
                throw new BadRequestException(ErrorMessages.BILLING_CYCLE_DAYS_REQUIRED);
            }
            billingChanged = true;
        }

        if (request.getBillingCycleDays() != null) {
            subscription.setBillingCycleDays(request.getBillingCycleDays());
            billingChanged = true;
        }

        if (request.getPaymentMethod() != null) {
            subscription.setPaymentMethod(request.getPaymentMethod());
        }

        if (request.getNextBillingDate() != null) {
            subscription.setNextBillingDate(request.getNextBillingDate());
            billingChanged = true;
        }

        if (request.getNotes() != null) {
            subscription.setNotes(request.getNotes());
        }

        // Recalculate start date if billing-related fields changed
        if (billingChanged) {
            recalculateStartDate(subscription);
        }

        subscription = subscriptionRepository.save(subscription);
        return SubscriptionResponse.fromEntity(subscription);
    }

    /**
     * Recalculates and sets the start date for a subscription based on its
     * current billing period and next billing date.
     *
     * @param subscription the subscription to update
     */
    private void recalculateStartDate(Subscription subscription) {
        BillingPeriod billingPeriod = subscription.getBillingPeriod();
        LocalDate calculatedStartDate = billingPeriod.calculateStartDate(subscription.getNextBillingDate());
        subscription.setStartDate(calculatedStartDate);
    }

    /**
     * Cancels an active subscription.
     *
     * <p>Delegates to the subscription entity's cancel method which sets the status
     * to cancelled and records the cancellation timestamp. Cancelled subscriptions
     * can be reactivated later.</p>
     *
     * @param userId         the ID of the user who owns the subscription
     * @param subscriptionId the ID of the subscription to cancel
     * @param request        the cancellation request with the cancellation date
     * @return the cancelled subscription response
     * @throws ResourceNotFoundException if the subscription is not found
     * @throws BadRequestException       if the subscription is already cancelled
     */
    @Transactional
    public SubscriptionResponse cancelSubscription(Long userId, Long subscriptionId, CancelSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_SUBSCRIPTION, DomainConstants.FIELD_ID, subscriptionId));

        subscription.cancel(request.getCancelledAt().atStartOfDay());

        subscription = subscriptionRepository.save(subscription);

        eventPublisher.publishEvent(new SubscriptionCancelledEvent(
                subscription.getId(),
                userId,
                subscription.getService().getName(),
                subscription.getCancelledAt()));

        return SubscriptionResponse.fromEntity(subscription);
    }

    /**
     * Reactivates a cancelled subscription.
     *
     * <p>Delegates to the subscription entity's reactivate method which sets
     * the status back to active, clears the cancellation timestamp, and sets
     * a new next billing date. The start date is recalculated based on the
     * new next billing date and the subscription's billing cycle.</p>
     *
     * @param userId         the ID of the user who owns the subscription
     * @param subscriptionId the ID of the subscription to reactivate
     * @param request        the reactivation request with the new billing date
     * @return the reactivated subscription response
     * @throws ResourceNotFoundException if the subscription is not found
     * @throws BadRequestException       if the subscription is already active
     */
    @Transactional
    public SubscriptionResponse reactivateSubscription(Long userId, Long subscriptionId, ReactivateSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_SUBSCRIPTION, DomainConstants.FIELD_ID, subscriptionId));

        subscription.reactivate(request.getNextBillingDate());

        // Recalculate start date after reactivation with new next billing date
        recalculateStartDate(subscription);

        subscription = subscriptionRepository.save(subscription);

        eventPublisher.publishEvent(new SubscriptionReactivatedEvent(
                subscription.getId(),
                userId,
                subscription.getService().getName(),
                subscription.getNextBillingDate()));

        return SubscriptionResponse.fromEntity(subscription);
    }

    /**
     * Permanently deletes a subscription and all associated payment records.
     *
     * @param userId         the ID of the user who owns the subscription
     * @param subscriptionId the ID of the subscription to delete
     * @throws ResourceNotFoundException if the subscription is not found
     */
    @Transactional
    public void deleteSubscription(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_SUBSCRIPTION, DomainConstants.FIELD_ID, subscriptionId));

        subscriptionRepository.delete(subscription);
    }

    /**
     * Retrieves all unique categories from the user's subscriptions.
     *
     * @param userId the ID of the user
     * @return list of distinct category names
     */
    public List<String> getCategories(Long userId) {
        return subscriptionRepository.findDistinctCategoriesByUserId(userId);
    }
}
