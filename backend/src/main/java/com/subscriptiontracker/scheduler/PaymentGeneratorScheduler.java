package com.subscriptiontracker.scheduler;

import com.subscriptiontracker.entity.*;
import com.subscriptiontracker.event.PaymentRecordCreatedEvent;
import com.subscriptiontracker.repository.PaymentRecordRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.service.FxRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentGeneratorScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final FxRateService fxRateService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 1 * * *") // Run at 1 AM every day
    @Transactional
    public void generatePaymentRecords() {
        log.info("Starting payment record generation job");

        LocalDate today = LocalDate.now();
        List<Subscription> dueSubscriptions = subscriptionRepository.findActiveSubscriptionsDueBefore(today.plusDays(1));

        int count = 0;
        for (Subscription subscription : dueSubscriptions) {
            try {
                createPaymentRecord(subscription, today);
                updateNextBillingDate(subscription);
                count++;
            } catch (Exception e) {
                log.error("Error processing subscription {}: {}", subscription.getId(), e.getMessage());
            }
        }

        log.info("Payment record generation completed. Processed {} subscriptions", count);
    }

    private void createPaymentRecord(Subscription subscription, LocalDate paymentDate) {
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

        payment = paymentRecordRepository.save(payment);

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
        LocalDate currentBillingDate = subscription.getNextBillingDate();
        LocalDate nextBillingDate;

        switch (subscription.getBillingCycle()) {
            case monthly:
                nextBillingDate = currentBillingDate.plusMonths(1);
                break;
            case yearly:
                nextBillingDate = currentBillingDate.plusYears(1);
                break;
            case bi_annual:
                nextBillingDate = currentBillingDate.plusMonths(6);
                break;
            case custom:
                int days = subscription.getBillingCycleDays() != null ? subscription.getBillingCycleDays() : 30;
                nextBillingDate = currentBillingDate.plusDays(days);
                break;
            default:
                nextBillingDate = currentBillingDate.plusMonths(1);
        }

        subscription.setNextBillingDate(nextBillingDate);
        subscriptionRepository.save(subscription);
    }
}
