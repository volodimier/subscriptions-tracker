package com.subscriptiontracker.scheduler;

import com.subscriptiontracker.entity.*;
import com.subscriptiontracker.event.PaymentRecordCreatedEvent;
import com.subscriptiontracker.repository.PaymentRecordRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.service.FxRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentGeneratorScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final FxRateService fxRateService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void generatePaymentRecords() {
        log.info("Starting payment record generation job");

        LocalDate utcToday = LocalDate.now(ZoneOffset.UTC);
        List<Subscription> dueSubscriptions = subscriptionRepository.findActiveSubscriptionsDueBefore(utcToday.plusDays(1));

        int count = 0;
        for (Subscription subscription : dueSubscriptions) {
            try {
                count += processSubscriptionDuePayments(subscription);
            } catch (Exception e) {
                log.error("Error processing subscription {}: {}", subscription.getId(), e.getMessage());
            }
        }

        log.info("Payment record generation completed. Processed {} subscriptions", count);
    }

    private int processSubscriptionDuePayments(Subscription subscription) {
        ZoneId userZone = resolveUserZone(subscription.getUser().getUserTimeZone());
        ZonedDateTime nowUser = ZonedDateTime.now(userZone);
        if (nowUser.toLocalTime().isBefore(LocalTime.of(0, 5))) {
            return 0;
        }

        LocalDate todayUser = nowUser.toLocalDate();
        int processed = 0;
        while (!subscription.getNextBillingDate().isAfter(todayUser)) {
            LocalDate chargeDate = subscription.getNextBillingDate();
            createPaymentRecordIdempotent(subscription, chargeDate);
            updateNextBillingDate(subscription);
            processed++;
        }
        return processed;
    }

    private ZoneId resolveUserZone(String userTimeZone) {
        try {
            return ZoneId.of(userTimeZone);
        } catch (Exception ex) {
            log.warn("Invalid user timezone '{}', falling back to UTC", userTimeZone);
            return ZoneOffset.UTC;
        }
    }

    private void createPaymentRecordIdempotent(Subscription subscription, LocalDate paymentDate) {
        User user = subscription.getUser();
        String fromCurrency = subscription.getCurrencyCode();
        String toCurrency = user.getBaseCurrencyCode();

        BigDecimal fxRate = fxRateService.getRate(fromCurrency, toCurrency, paymentDate);
        BigDecimal amountInBase = subscription.getAmount()
                .multiply(fxRate)
                .setScale(2, RoundingMode.HALF_UP);

        PaymentRecord payment = PaymentRecord.builder()
                .subscription(subscription)
                .amount(subscription.getAmount())
                .currencyCode(subscription.getCurrencyCode())
                .paymentDate(paymentDate)
                .fxRateToBase(fxRate)
                .amountInBaseCurrency(amountInBase)
                .build();

        try {
            payment = paymentRecordRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Payment already exists for subscription {} on {}, skipping duplicate insert",
                    subscription.getId(), paymentDate);
            return;
        }

        eventPublisher.publishEvent(new PaymentRecordCreatedEvent(
                payment.getId(),
                subscription.getId(),
                user.getId(),
                subscription.getService().getName(),
                payment.getAmount(),
                payment.getCurrencyCode(),
                payment.getAmountInBaseCurrency(),
                payment.getPaymentDate()));

        log.debug("Created payment record for subscription {} on {}", subscription.getId(), paymentDate);
    }

    private void updateNextBillingDate(Subscription subscription) {
        if (subscription.getBillingCycle() == BillingCycle.custom) {
            int days = subscription.getBillingCycleDays() != null && subscription.getBillingCycleDays() > 0
                    ? subscription.getBillingCycleDays()
                    : 30;
            subscription.setNextBillingDate(subscription.getNextBillingDate().plusDays(days));
        } else {
            subscription.advanceToNextBillingDate();
        }
        subscriptionRepository.save(subscription);
    }
}
