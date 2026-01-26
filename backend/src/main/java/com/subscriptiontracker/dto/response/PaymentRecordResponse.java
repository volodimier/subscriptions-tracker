package com.subscriptiontracker.dto.response;

import com.subscriptiontracker.entity.PaymentRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecordResponse {
    private Long id;
    private Long subscriptionId;
    private BigDecimal amount;
    private String currencyCode;
    private LocalDate paymentDate;
    private BigDecimal fxRateToBase;
    private BigDecimal amountInBaseCurrency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentRecordResponse fromEntity(PaymentRecord payment) {
        return PaymentRecordResponse.builder()
                .id(payment.getId())
                .subscriptionId(payment.getSubscription().getId())
                .amount(payment.getAmount())
                .currencyCode(payment.getCurrencyCode())
                .paymentDate(payment.getPaymentDate())
                .fxRateToBase(payment.getFxRateToBase())
                .amountInBaseCurrency(payment.getAmountInBaseCurrency())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
