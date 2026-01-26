package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.request.CancelSubscriptionRequest;
import com.subscriptiontracker.dto.request.CreateSubscriptionRequest;
import com.subscriptiontracker.dto.request.ReactivateSubscriptionRequest;
import com.subscriptiontracker.dto.request.UpdateSubscriptionRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.SubscriptionDetailResponse;
import com.subscriptiontracker.dto.response.SubscriptionResponse;
import com.subscriptiontracker.entity.*;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.PaymentRecordRepository;
import com.subscriptiontracker.repository.ServiceRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final PaymentRecordRepository paymentRecordRepository;

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

    public SubscriptionDetailResponse getSubscriptionDetail(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

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

    @Transactional
    public SubscriptionResponse createSubscription(Long userId, CreateSubscriptionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Service service = serviceRepository.findByIdAndUserId(request.getServiceId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", request.getServiceId()));

        if (request.getBillingCycle() == BillingCycle.custom && request.getBillingCycleDays() == null) {
            throw new BadRequestException("Billing cycle days is required for custom billing cycle");
        }

        Subscription subscription = Subscription.builder()
                .user(user)
                .service(service)
                .amount(request.getAmount())
                .currencyCode(request.getCurrencyCode())
                .billingCycle(request.getBillingCycle())
                .billingCycleDays(request.getBillingCycleDays())
                .paymentMethod(request.getPaymentMethod())
                .startDate(request.getStartDate())
                .nextBillingDate(request.getNextBillingDate())
                .notes(request.getNotes())
                .status(SubscriptionStatus.active)
                .build();

        subscription = subscriptionRepository.save(subscription);
        return SubscriptionResponse.fromEntity(subscription);
    }

    @Transactional
    public SubscriptionResponse updateSubscription(Long userId, Long subscriptionId, UpdateSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        if (request.getServiceId() != null) {
            Service service = serviceRepository.findByIdAndUserId(request.getServiceId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "id", request.getServiceId()));
            subscription.setService(service);
        }

        if (request.getAmount() != null) {
            subscription.setAmount(request.getAmount());
        }

        if (request.getCurrencyCode() != null) {
            subscription.setCurrencyCode(request.getCurrencyCode());
        }

        if (request.getBillingCycle() != null) {
            subscription.setBillingCycle(request.getBillingCycle());
            if (request.getBillingCycle() == BillingCycle.custom && request.getBillingCycleDays() == null) {
                throw new BadRequestException("Billing cycle days is required for custom billing cycle");
            }
        }

        if (request.getBillingCycleDays() != null) {
            subscription.setBillingCycleDays(request.getBillingCycleDays());
        }

        if (request.getPaymentMethod() != null) {
            subscription.setPaymentMethod(request.getPaymentMethod());
        }

        if (request.getNextBillingDate() != null) {
            subscription.setNextBillingDate(request.getNextBillingDate());
        }

        if (request.getNotes() != null) {
            subscription.setNotes(request.getNotes());
        }

        subscription = subscriptionRepository.save(subscription);
        return SubscriptionResponse.fromEntity(subscription);
    }

    @Transactional
    public SubscriptionResponse cancelSubscription(Long userId, Long subscriptionId, CancelSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        if (subscription.getStatus() == SubscriptionStatus.cancelled) {
            throw new BadRequestException("Subscription is already cancelled");
        }

        subscription.setStatus(SubscriptionStatus.cancelled);
        subscription.setCancelledAt(request.getCancelledAt().atStartOfDay());

        subscription = subscriptionRepository.save(subscription);
        return SubscriptionResponse.fromEntity(subscription);
    }

    @Transactional
    public SubscriptionResponse reactivateSubscription(Long userId, Long subscriptionId, ReactivateSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        if (subscription.getStatus() == SubscriptionStatus.active) {
            throw new BadRequestException("Subscription is already active");
        }

        subscription.setStatus(SubscriptionStatus.active);
        subscription.setCancelledAt(null);
        subscription.setNextBillingDate(request.getNextBillingDate());

        subscription = subscriptionRepository.save(subscription);
        return SubscriptionResponse.fromEntity(subscription);
    }

    @Transactional
    public void deleteSubscription(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        subscriptionRepository.delete(subscription);
    }

    public List<String> getCategories(Long userId) {
        return subscriptionRepository.findDistinctCategoriesByUserId(userId);
    }
}
