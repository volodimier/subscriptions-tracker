package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.request.CreatePaymentRequest;
import com.subscriptiontracker.dto.request.UpdatePaymentRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.PaymentRecordResponse;
import com.subscriptiontracker.entity.PaymentRecord;
import com.subscriptiontracker.entity.Subscription;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.PaymentRecordRepository;
import com.subscriptiontracker.repository.SubscriptionRepository;
import com.subscriptiontracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentRecordService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final FxRateService fxRateService;

    public PaginatedResponse<PaymentRecordResponse> getPaymentsForSubscription(
            Long userId, Long subscriptionId, int page, int limit) {

        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by("paymentDate").descending());
        Page<PaymentRecord> paymentPage = paymentRecordRepository.findBySubscriptionIdOrderByPaymentDateDesc(
                subscriptionId, pageRequest);

        List<PaymentRecordResponse> payments = paymentPage.getContent().stream()
                .map(PaymentRecordResponse::fromEntity)
                .collect(Collectors.toList());

        return PaginatedResponse.of(payments, page, limit, paymentPage.getTotalElements());
    }

    @Transactional
    public PaymentRecordResponse createPayment(Long userId, CreatePaymentRequest request) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(request.getSubscriptionId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", request.getSubscriptionId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

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
        return PaymentRecordResponse.fromEntity(payment);
    }

    @Transactional
    public PaymentRecordResponse updatePayment(Long userId, Long paymentId, UpdatePaymentRequest request) {
        PaymentRecord payment = paymentRecordRepository.findByIdAndSubscriptionUserId(paymentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentRecord", "id", paymentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

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

    @Transactional
    public void deletePayment(Long userId, Long paymentId) {
        PaymentRecord payment = paymentRecordRepository.findByIdAndSubscriptionUserId(paymentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentRecord", "id", paymentId));

        paymentRecordRepository.delete(payment);
    }
}
