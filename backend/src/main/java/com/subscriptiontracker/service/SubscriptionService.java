package com.subscriptiontracker.service;

import com.subscriptiontracker.constant.DomainConstants;
import com.subscriptiontracker.constant.ErrorMessages;
import com.subscriptiontracker.constant.RecurrenceValidation;
import com.subscriptiontracker.dto.request.CancelSubscriptionRequest;
import com.subscriptiontracker.dto.request.CreateSubscriptionRequest;
import com.subscriptiontracker.dto.request.ReactivateSubscriptionRequest;
import com.subscriptiontracker.dto.request.UpdateSubscriptionRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.SubscriptionDetailResponse;
import com.subscriptiontracker.dto.response.SubscriptionHistoryResponse;
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
import com.subscriptiontracker.repository.SubscriptionHistoryRepository;
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
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
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
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final FxRateService fxRateService;
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

        if (request.getBillingCycle() != BillingCycle.monthly && request.getBillingCycle() != BillingCycle.yearly) {
            throw recurrenceBadRequest(
                    "Billing cycle is not supported. Only monthly and yearly billing cycles are allowed.",
                    RecurrenceValidation.RULE_VAL_REC_005,
                    RecurrenceValidation.CODE_CADENCE_NOT_SUPPORTED,
                    "billingCycle"
            );
        }

        ResolvedRecurrence resolvedRecurrence = resolveRecurrence(user, request);

        LocalDate startDate = calculateStartDate(request.getBillingCycle(), resolvedRecurrence);

        Subscription subscription = Subscription.builder()
                .user(user)
                .service(service)
                .amount(request.getAmount())
                .currencyCode(request.getCurrencyCode())
                .billingCycle(request.getBillingCycle())
                .billingCycleDays(null)
                .paymentMethod(request.getPaymentMethod())
                .startDate(startDate)
                .nextBillingDate(resolvedRecurrence.nextBillingDate())
                .anchorDay(resolvedRecurrence.anchorDay())
                .anchorMonth(resolvedRecurrence.anchorMonth())
                .timeZoneAtCreation(resolvedRecurrence.userTimeZone())
                .notes(request.getNotes())
                .status(SubscriptionStatus.active)
                .build();

        subscription = subscriptionRepository.save(subscription);
        backfillPaymentsIfNeeded(subscription, resolvedRecurrence);

        createHistorySnapshot(subscription);

        eventPublisher.publishEvent(new SubscriptionCreatedEvent(
                subscription.getId(),
                userId,
                service.getId(),
                service.getName(),
                subscription.getAmount(),
                subscription.getCurrencyCode()));

        return SubscriptionResponse.fromEntity(subscription);
    }

    private LocalDate calculateStartDate(BillingCycle billingCycle, ResolvedRecurrence resolvedRecurrence) {
        if (resolvedRecurrence.startDate() != null) {
            return resolvedRecurrence.startDate();
        }

        Integer anchorDay = resolvedRecurrence.anchorDay();
        if (anchorDay != null && (billingCycle == BillingCycle.monthly || billingCycle == BillingCycle.yearly)) {
            return rewindWithAnchor(
                    resolvedRecurrence.nextBillingDate(),
                    billingCycle,
                    anchorDay,
                    resolvedRecurrence.anchorMonth()
            );
        }

        BillingPeriod billingPeriod = BillingPeriod.of(billingCycle, null);
        return billingPeriod.calculateStartDate(resolvedRecurrence.nextBillingDate());
    }

    private record ResolvedRecurrence(
            LocalDate nextBillingDate,
            LocalDate startDate,
            Integer anchorDay,
            Integer anchorMonth,
            String userTimeZone,
            LocalDate firstBillingDateForBackfill,
            LocalDate backfillCutoffDate
    ) { }

    private ResolvedRecurrence resolveRecurrence(User user, CreateSubscriptionRequest request) {
        ZoneId userZone = resolveUserZone(user.getUserTimeZone());
        LocalDate cutoffDate = calculateCutoffDate(userZone);

        LocalDate firstBillingDate = request.getFirstBillingDate();
        LocalDate nextBillingDate = request.getNextBillingDate();
        Integer anchorDayInput = request.getAnchorDay();
        String anchorMonthDayInput = normalizeAnchorMonthDay(request.getAnchorMonthDay());

        if (firstBillingDate == null && nextBillingDate == null) {
            throw recurrenceBadRequest(
                    "Either FirstBillDate or NextBillDate is required.",
                    RecurrenceValidation.RULE_VAL_REC_001,
                    RecurrenceValidation.CODE_DATE_REQUIRED,
                    "firstBillDate,nextBillDate"
            );
        }

        if (firstBillingDate != null && firstBillingDate.isAfter(cutoffDate)) {
            throw recurrenceBadRequest(
                    "FirstBillDate cannot be in the future relative to your local cutoff date.",
                    RecurrenceValidation.RULE_VAL_REC_002,
                    RecurrenceValidation.CODE_FIRST_DATE_AFTER_CUTOFF,
                    "firstBillDate"
            );
        }

        if (firstBillingDate != null && nextBillingDate != null && firstBillingDate.isAfter(nextBillingDate)) {
            throw recurrenceBadRequest(
                    "FirstBillDate must be less than or equal to NextBillDate.",
                    RecurrenceValidation.RULE_VAL_REC_003,
                    RecurrenceValidation.CODE_FIRST_AFTER_NEXT,
                    "firstBillDate,nextBillDate"
            );
        }

        if (firstBillingDate != null) {
            if (anchorDayInput != null || anchorMonthDayInput != null) {
                throw recurrenceBadRequest(
                        "Anchor override is not allowed when FirstBillDate is provided.",
                        nextBillingDate != null ? RecurrenceValidation.RULE_VAL_REC_007
                                : RecurrenceValidation.RULE_VAL_REC_008,
                        RecurrenceValidation.CODE_ANCHOR_OVERRIDE_NOT_ALLOWED,
                        "anchorDay,anchorMonthDay"
                );
            }

            int anchorDay = firstBillingDate.getDayOfMonth();
            Integer anchorMonth = request.getBillingCycle() == BillingCycle.yearly
                    ? firstBillingDate.getMonthValue()
                    : null;

            LocalDate expectedNext = computeExpectedNext(firstBillingDate, cutoffDate, request.getBillingCycle(), anchorDay,
                    anchorMonth);
            if (nextBillingDate != null && !nextBillingDate.equals(expectedNext)) {
                throw recurrenceBadRequest(
                        "Dates don't match a standard monthly/yearly billing schedule.",
                        RecurrenceValidation.RULE_VAL_REC_004,
                        RecurrenceValidation.CODE_NEXT_DATE_MISMATCH,
                        "nextBillDate"
                );
            }

            LocalDate resolvedNext = nextBillingDate != null ? nextBillingDate : expectedNext;
            return new ResolvedRecurrence(
                    resolvedNext,
                    firstBillingDate,
                    anchorDay,
                    anchorMonth,
                    userZone.getId(),
                    firstBillingDate,
                    cutoffDate
            );
        }

        if (request.getBillingCycle() == BillingCycle.monthly) {
            return resolveMonthlyNextOnly(nextBillingDate, anchorDayInput, anchorMonthDayInput, userZone.getId());
        }

        return resolveYearlyNextOnly(nextBillingDate, anchorDayInput, anchorMonthDayInput, userZone.getId());
    }

    private ResolvedRecurrence resolveMonthlyNextOnly(
            LocalDate nextBillingDate,
            Integer anchorDayInput,
            String anchorMonthDayInput,
            String userTimeZone
    ) {
        if (anchorMonthDayInput != null) {
            throw recurrenceBadRequest(
                    "anchorMonthDay is not allowed for monthly cadence.",
                    RecurrenceValidation.RULE_VAL_REC_011,
                    RecurrenceValidation.CODE_ANCHOR_NOT_ALLOWED,
                    "anchorMonthDay"
            );
        }

        Set<Integer> allowedAnchors = null;
        String ruleIdForAmbiguity = null;
        if (nextBillingDate.getMonthValue() == 2 && nextBillingDate.getDayOfMonth() == 28) {
            allowedAnchors = Set.of(28, 29, 30, 31);
            ruleIdForAmbiguity = RecurrenceValidation.RULE_VAL_REC_M_001;
        } else if (nextBillingDate.getMonthValue() == 2 && nextBillingDate.getDayOfMonth() == 29) {
            allowedAnchors = Set.of(29, 30, 31);
            ruleIdForAmbiguity = RecurrenceValidation.RULE_VAL_REC_M_002;
        } else if (nextBillingDate.getDayOfMonth() == 30) {
            allowedAnchors = Set.of(30, 31);
            ruleIdForAmbiguity = RecurrenceValidation.RULE_VAL_REC_M_003;
        }

        int resolvedAnchorDay;
        if (allowedAnchors == null) {
            if (anchorDayInput != null) {
                throw recurrenceBadRequest(
                        "anchorDay is only allowed for ambiguous monthly next billing dates.",
                        RecurrenceValidation.RULE_VAL_REC_011,
                        RecurrenceValidation.CODE_ANCHOR_NOT_ALLOWED,
                        "anchorDay"
                );
            }
            resolvedAnchorDay = nextBillingDate.getDayOfMonth();
        } else {
            if (anchorDayInput == null) {
                throw recurrenceBadRequest(
                        "Monthly anchorDay is required for this next billing date.",
                        RecurrenceValidation.RULE_VAL_REC_010,
                        RecurrenceValidation.CODE_MONTHLY_ANCHOR_REQUIRED,
                        "anchorDay",
                        allowedAnchors.stream()
                                .sorted()
                                .map(String::valueOf)
                                .collect(Collectors.joining(","))
                );
            }
            if (anchorDayInput < 1 || anchorDayInput > 31) {
                throw recurrenceBadRequest(
                        "Monthly anchorDay must be within 1..31.",
                        RecurrenceValidation.RULE_VAL_REC_M_006,
                        RecurrenceValidation.CODE_MONTHLY_ANCHOR_OUT_OF_RANGE,
                        "anchorDay"
                );
            }
            if (!allowedAnchors.contains(anchorDayInput)) {
                throw recurrenceBadRequest(
                        "Monthly anchorDay is not valid for this next billing date.",
                        ruleIdForAmbiguity,
                        RecurrenceValidation.CODE_MONTHLY_ANCHOR_INVALID,
                        "anchorDay",
                        allowedAnchors.stream()
                                .sorted()
                                .map(String::valueOf)
                                .collect(Collectors.joining(","))
                );
            }
            resolvedAnchorDay = anchorDayInput;
        }

        return new ResolvedRecurrence(
                nextBillingDate,
                null,
                resolvedAnchorDay,
                null,
                userTimeZone,
                null,
                null
        );
    }

    private ResolvedRecurrence resolveYearlyNextOnly(
            LocalDate nextBillingDate,
            Integer anchorDayInput,
            String anchorMonthDayInput,
            String userTimeZone
    ) {
        if (anchorDayInput != null) {
            throw recurrenceBadRequest(
                    "anchorDay is not allowed for yearly cadence.",
                    RecurrenceValidation.RULE_VAL_REC_011,
                    RecurrenceValidation.CODE_ANCHOR_NOT_ALLOWED,
                    "anchorDay"
            );
        }

        int resolvedAnchorDay;
        int resolvedAnchorMonth;
        boolean ambiguous = nextBillingDate.getMonthValue() == 2 && nextBillingDate.getDayOfMonth() == 28;
        if (!ambiguous) {
            if (anchorMonthDayInput != null) {
                throw recurrenceBadRequest(
                        "anchorMonthDay is only allowed for ambiguous yearly next billing dates.",
                        RecurrenceValidation.RULE_VAL_REC_011,
                        RecurrenceValidation.CODE_ANCHOR_NOT_ALLOWED,
                        "anchorMonthDay"
                );
            }
            resolvedAnchorMonth = nextBillingDate.getMonthValue();
            resolvedAnchorDay = nextBillingDate.getDayOfMonth();
        } else {
            if (anchorMonthDayInput == null) {
                throw recurrenceBadRequest(
                        "Yearly anchorMonthDay is required for Feb 28 next billing date.",
                        RecurrenceValidation.RULE_VAL_REC_010,
                        RecurrenceValidation.CODE_YEARLY_ANCHOR_REQUIRED,
                        "anchorMonthDay",
                        "02-28,02-29"
                );
            }

            int[] parsedAnchor = parseYearlyAnchorMonthDay(anchorMonthDayInput);
            if (parsedAnchor == null) {
                throw recurrenceBadRequest(
                        "Yearly anchorMonthDay must have MM-dd format and a valid calendar day.",
                        RecurrenceValidation.RULE_VAL_REC_Y_004,
                        RecurrenceValidation.CODE_YEARLY_ANCHOR_FORMAT_INVALID,
                        "anchorMonthDay"
                );
            }

            if (!(parsedAnchor[0] == 2 && (parsedAnchor[1] == 28 || parsedAnchor[1] == 29))) {
                throw recurrenceBadRequest(
                        "Yearly anchorMonthDay is not valid for this next billing date.",
                        RecurrenceValidation.RULE_VAL_REC_Y_001,
                        RecurrenceValidation.CODE_YEARLY_ANCHOR_INVALID,
                        "anchorMonthDay",
                        "02-28,02-29"
                );
            }

            resolvedAnchorMonth = parsedAnchor[0];
            resolvedAnchorDay = parsedAnchor[1];
        }

        return new ResolvedRecurrence(
                nextBillingDate,
                null,
                resolvedAnchorDay,
                resolvedAnchorMonth,
                userTimeZone,
                null,
                null
        );
    }

    private LocalDate computeExpectedNext(
            LocalDate firstBillingDate,
            LocalDate cutoffDate,
            BillingCycle billingCycle,
            int anchorDay,
            Integer anchorMonth
    ) {
        LocalDate occurrence = firstBillingDate;
        while (!occurrence.isAfter(cutoffDate)) {
            occurrence = advanceWithAnchor(occurrence, billingCycle, anchorDay, anchorMonth);
        }
        return occurrence;
    }

    private LocalDate advanceWithAnchor(
            LocalDate currentDate,
            BillingCycle billingCycle,
            int anchorDay,
            Integer anchorMonth
    ) {
        if (billingCycle == BillingCycle.monthly) {
            YearMonth targetMonth = YearMonth.from(currentDate).plusMonths(1);
            int day = Math.min(anchorDay, targetMonth.lengthOfMonth());
            return targetMonth.atDay(day);
        }
        if (billingCycle == BillingCycle.yearly) {
            int year = currentDate.getYear() + 1;
            YearMonth targetMonth = YearMonth.of(year, anchorMonth);
            int day = Math.min(anchorDay, targetMonth.lengthOfMonth());
            return targetMonth.atDay(day);
        }
        throw recurrenceBadRequest(
                "Billing cycle is not supported. Only monthly and yearly billing cycles are allowed.",
                RecurrenceValidation.RULE_VAL_REC_005,
                RecurrenceValidation.CODE_CADENCE_NOT_SUPPORTED,
                "billingCycle"
        );
    }

    private LocalDate rewindWithAnchor(
            LocalDate currentDate,
            BillingCycle billingCycle,
            int anchorDay,
            Integer anchorMonth
    ) {
        if (billingCycle == BillingCycle.monthly) {
            YearMonth targetMonth = YearMonth.from(currentDate).minusMonths(1);
            int day = Math.min(anchorDay, targetMonth.lengthOfMonth());
            return targetMonth.atDay(day);
        }
        if (billingCycle == BillingCycle.yearly) {
            int year = currentDate.getYear() - 1;
            int month = anchorMonth != null ? anchorMonth : currentDate.getMonthValue();
            YearMonth targetMonth = YearMonth.of(year, month);
            int day = Math.min(anchorDay, targetMonth.lengthOfMonth());
            return targetMonth.atDay(day);
        }
        throw recurrenceBadRequest(
                "Billing cycle is not supported. Only monthly and yearly billing cycles are allowed.",
                RecurrenceValidation.RULE_VAL_REC_005,
                RecurrenceValidation.CODE_CADENCE_NOT_SUPPORTED,
                "billingCycle"
        );
    }

    private void backfillPaymentsIfNeeded(Subscription subscription, ResolvedRecurrence resolvedRecurrence) {
        LocalDate firstBillingDate = resolvedRecurrence.firstBillingDateForBackfill();
        LocalDate cutoffDate = resolvedRecurrence.backfillCutoffDate();
        if (firstBillingDate == null || cutoffDate == null) {
            return;
        }

        LocalDate chargeDate = firstBillingDate;
        while (!chargeDate.isAfter(cutoffDate)) {
            createBackfillPaymentIfAbsent(subscription, chargeDate);
            chargeDate = advanceWithAnchor(
                    chargeDate,
                    subscription.getBillingCycle(),
                    resolvedRecurrence.anchorDay(),
                    resolvedRecurrence.anchorMonth()
            );
        }
    }

    private void createBackfillPaymentIfAbsent(Subscription subscription, LocalDate chargeDate) {
        String subscriptionCurrency = subscription.getCurrencyCode();
        String baseCurrency = subscription.getUser().getBaseCurrencyCode();

        BigDecimal fxRate = fxRateService.getRate(subscriptionCurrency, baseCurrency, chargeDate);
        BigDecimal amountInBaseCurrency = subscription.getAmount()
                .multiply(fxRate)
                .setScale(2, RoundingMode.HALF_UP);

        paymentRecordRepository.insertIfAbsent(
                subscription.getId(),
                subscription.getAmount(),
                subscriptionCurrency,
                chargeDate,
                fxRate,
                amountInBaseCurrency,
                PaymentStatus.paid.name(),
                chargeDate
        );
    }

    private ZoneId resolveUserZone(String userTimeZone) {
        if (userTimeZone == null || userTimeZone.isBlank()) {
            throw recurrenceBadRequest(
                    "User timezone must be configured before recurrence processing.",
                    RecurrenceValidation.RULE_VAL_REC_006,
                    RecurrenceValidation.CODE_USER_TIMEZONE_INVALID,
                    "userTimeZone"
            );
        }
        try {
            return ZoneId.of(userTimeZone);
        } catch (DateTimeException ex) {
            throw recurrenceBadRequest(
                    "User timezone must be a valid IANA timezone ID.",
                    RecurrenceValidation.RULE_VAL_REC_006,
                    RecurrenceValidation.CODE_USER_TIMEZONE_INVALID,
                    "userTimeZone"
            );
        }
    }

    private LocalDate calculateCutoffDate(ZoneId userZone) {
        ZonedDateTime nowUser = ZonedDateTime.now(userZone);
        LocalDate todayUser = nowUser.toLocalDate();
        return nowUser.toLocalTime().isBefore(LocalTime.of(0, 5))
                ? todayUser.minusDays(1)
                : todayUser;
    }

    private String normalizeAnchorMonthDay(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int[] parseYearlyAnchorMonthDay(String anchorMonthDay) {
        if (!anchorMonthDay.matches("\\d{2}-\\d{2}")) {
            return null;
        }

        int month;
        int day;
        try {
            month = Integer.parseInt(anchorMonthDay.substring(0, 2));
            day = Integer.parseInt(anchorMonthDay.substring(3, 5));
        } catch (NumberFormatException ex) {
            return null;
        }

        if (month < 1 || month > 12 || day < 1 || day > 31) {
            return null;
        }

        YearMonth monthInLeapYear;
        try {
            monthInLeapYear = YearMonth.of(2024, month); // allows Feb 29 as valid yearly anchor
        } catch (DateTimeException ex) {
            return null;
        }

        if (day > monthInLeapYear.lengthOfMonth()) {
            return null;
        }

        return new int[] {month, day};
    }

    private BadRequestException recurrenceBadRequest(String message, String ruleId, String code, String field) {
        return new BadRequestException(message, RecurrenceValidation.details(ruleId, code, field));
    }

    private BadRequestException recurrenceBadRequest(
            String message,
            String ruleId,
            String code,
            String field,
            String allowedValues
    ) {
        return new BadRequestException(message, RecurrenceValidation.details(ruleId, code, field, allowedValues));
    }

    /**
     * Updates an existing subscription.
     *
     * <p>Only non-null fields in the request are updated. The subscription
     * must belong to the specified user.</p>
     *
     * <p>Billing cycle and billing cycle days cannot be changed on an existing
     * subscription. If the user needs a different billing cycle, they must cancel
     * the current subscription and create a new one.</p>
     *
     * <p>Before applying changes, a history snapshot of the current state is saved
     * to preserve the audit trail of changes over time.</p>
     *
     * <p>If the next billing date is changed, the start date is automatically
     * recalculated to maintain consistency.</p>
     *
     * @param userId         the ID of the user who owns the subscription
     * @param subscriptionId the ID of the subscription to update
     * @param request        the update request with fields to modify
     * @return the updated subscription response
     * @throws ResourceNotFoundException if the subscription or service is not found
     * @throws BadRequestException       if an attempt is made to change billing cycle or billing cycle days
     */
    @Transactional
    public SubscriptionResponse updateSubscription(Long userId, Long subscriptionId, UpdateSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_SUBSCRIPTION, DomainConstants.FIELD_ID, subscriptionId));

        // Billing cycle changes are not allowed on existing subscriptions
        if (request.getBillingCycle() != null || request.getBillingCycleDays() != null) {
            throw new BadRequestException(ErrorMessages.BILLING_CYCLE_CHANGE_NOT_ALLOWED);
        }

        // Snapshot the current state before applying changes
        createHistorySnapshot(subscription);

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

        if (request.getPaymentMethod() != null) {
            subscription.setPaymentMethod(request.getPaymentMethod());
        }

        // Track if billing-related fields changed to trigger start date recalculation
        boolean billingChanged = false;

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

    /**
     * Retrieves the change history for a subscription.
     *
     * <p>Returns a list of history snapshots ordered by changed_at descending
     * (most recent first). Verifies that the subscription belongs to the specified user.</p>
     *
     * @param userId         the ID of the user who owns the subscription
     * @param subscriptionId the ID of the subscription
     * @return list of history response DTOs ordered by changedAt descending
     * @throws ResourceNotFoundException if the subscription is not found or does not belong to the user
     */
    public List<SubscriptionHistoryResponse> getSubscriptionHistory(Long userId, Long subscriptionId) {
        subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_SUBSCRIPTION, DomainConstants.FIELD_ID, subscriptionId));

        return subscriptionHistoryRepository.findBySubscriptionIdOrderByChangedAtDesc(subscriptionId)
                .stream()
                .map(SubscriptionHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Creates a history snapshot of the subscription's current mutable fields.
     *
     * <p>Records the current values of amount, currency code, payment method,
     * and notes for the given subscription. This method is called during
     * subscription creation (initial snapshot) and before updates (pre-change snapshot).</p>
     *
     * @param subscription the subscription to snapshot
     */
    private void createHistorySnapshot(Subscription subscription) {
        SubscriptionHistory snapshot = SubscriptionHistory.builder()
                .subscription(subscription)
                .changedAt(LocalDateTime.now())
                .amount(subscription.getAmount())
                .currencyCode(subscription.getCurrencyCode())
                .paymentMethod(subscription.getPaymentMethod())
                .notes(subscription.getNotes())
                .build();

        subscriptionHistoryRepository.save(snapshot);
    }
}
