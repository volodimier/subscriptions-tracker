package com.subscriptiontracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptiontracker.dto.request.CreatePaymentRequest;
import com.subscriptiontracker.dto.request.UpdatePaymentRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.PaymentRecordResponse;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.FxRateService;
import com.subscriptiontracker.service.JobRunService;
import com.subscriptiontracker.service.JwtService;
import com.subscriptiontracker.service.PaymentRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link PaymentRecordController}.
 *
 * <p>Tests all payment record management endpoints including CRUD operations
 * and FX rate refresh functionality.</p>
 *
 * @author Generated
 * @since 1.0
 */
@WebMvcTest(PaymentRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PaymentRecordController")
class PaymentRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentRecordService paymentRecordService;

    @MockBean
    private FxRateService fxRateService;

    @MockBean
    private JobRunService jobRunService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private CurrentUserService currentUserService;

    private static final Long USER_ID = 1L;
    private static final Long SUBSCRIPTION_ID = 100L;
    private static final Long PAYMENT_ID = 200L;

    private PaymentRecordResponse paymentResponse;
    private CreatePaymentRequest validCreateRequest;
    private UpdatePaymentRequest validUpdateRequest;

    @BeforeEach
    void setUp() {
        when(currentUserService.getCurrentUserId(any())).thenReturn(USER_ID);

        paymentResponse = PaymentRecordResponse.builder()
                .id(PAYMENT_ID)
                .subscriptionId(SUBSCRIPTION_ID)
                .amount(new BigDecimal("15.99"))
                .currencyCode("USD")
                .paymentDate(LocalDate.of(2024, 1, 15))
                .fxRateToBase(new BigDecimal("1.000000"))
                .amountInBaseCurrency(new BigDecimal("15.99"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validCreateRequest = CreatePaymentRequest.builder()
                .subscriptionId(SUBSCRIPTION_ID)
                .amount(new BigDecimal("15.99"))
                .currencyCode("USD")
                .paymentDate(LocalDate.of(2024, 1, 15))
                .fxRateToBase(new BigDecimal("1.000000"))
                .build();

        validUpdateRequest = UpdatePaymentRequest.builder()
                .amount(new BigDecimal("19.99"))
                .currencyCode("EUR")
                .paymentDate(LocalDate.of(2024, 1, 20))
                .fxRateToBase(new BigDecimal("0.920000"))
                .build();
    }

    @Nested
    @DisplayName("GET /subscriptions/{subscriptionId}/payments")
    class GetPaymentsForSubscription {

        @Test
        @DisplayName("should return 200 with paginated payments")
        void shouldReturn200WithPaginatedPayments() throws Exception {
            PaginatedResponse<PaymentRecordResponse> paginatedResponse = PaginatedResponse.of(
                    List.of(paymentResponse), 1, 20, 1
            );

            when(paymentRecordService.getPaymentsForSubscription(USER_ID, SUBSCRIPTION_ID, 1, 20))
                    .thenReturn(paginatedResponse);

            mockMvc.perform(get("/subscriptions/{subscriptionId}/payments", SUBSCRIPTION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(PAYMENT_ID))
                    .andExpect(jsonPath("$.data[0].subscriptionId").value(SUBSCRIPTION_ID))
                    .andExpect(jsonPath("$.data[0].amount").value(15.99))
                    .andExpect(jsonPath("$.data[0].currencyCode").value("USD"))
                    .andExpect(jsonPath("$.data[0].paymentDate").value("2024-01-15"))
                    .andExpect(jsonPath("$.data[0].fxRateToBase").value(1.000000))
                    .andExpect(jsonPath("$.data[0].amountInBaseCurrency").value(15.99))
                    .andExpect(jsonPath("$.pagination.page").value(1))
                    .andExpect(jsonPath("$.pagination.limit").value(20))
                    .andExpect(jsonPath("$.pagination.total").value(1));
        }

        @Test
        @DisplayName("should return 200 with custom pagination")
        void shouldReturn200WithCustomPagination() throws Exception {
            PaginatedResponse<PaymentRecordResponse> paginatedResponse = PaginatedResponse.of(
                    List.of(paymentResponse), 2, 10, 15
            );

            when(paymentRecordService.getPaymentsForSubscription(USER_ID, SUBSCRIPTION_ID, 2, 10))
                    .thenReturn(paginatedResponse);

            mockMvc.perform(get("/subscriptions/{subscriptionId}/payments", SUBSCRIPTION_ID)
                            .param("page", "2")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pagination.page").value(2))
                    .andExpect(jsonPath("$.pagination.limit").value(10));
        }

        @Test
        @DisplayName("should return 200 with empty list when no payments")
        void shouldReturn200WithEmptyListWhenNoPayments() throws Exception {
            PaginatedResponse<PaymentRecordResponse> emptyResponse = PaginatedResponse.of(
                    List.of(), 1, 20, 0
            );

            when(paymentRecordService.getPaymentsForSubscription(USER_ID, SUBSCRIPTION_ID, 1, 20))
                    .thenReturn(emptyResponse);

            mockMvc.perform(get("/subscriptions/{subscriptionId}/payments", SUBSCRIPTION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.pagination.total").value(0));
        }

        @Test
        @DisplayName("should return 404 when subscription not found")
        void shouldReturn404WhenSubscriptionNotFound() throws Exception {
            when(paymentRecordService.getPaymentsForSubscription(USER_ID, 999L, 1, 20))
                    .thenThrow(new ResourceNotFoundException("Subscription", "id", 999L));

            mockMvc.perform(get("/subscriptions/{subscriptionId}/payments", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 200 with multiple payments")
        void shouldReturn200WithMultiplePayments() throws Exception {
            PaymentRecordResponse payment2 = PaymentRecordResponse.builder()
                    .id(201L)
                    .subscriptionId(SUBSCRIPTION_ID)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .paymentDate(LocalDate.of(2024, 2, 15))
                    .fxRateToBase(new BigDecimal("1.000000"))
                    .amountInBaseCurrency(new BigDecimal("15.99"))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            PaginatedResponse<PaymentRecordResponse> paginatedResponse = PaginatedResponse.of(
                    List.of(paymentResponse, payment2), 1, 20, 2
            );

            when(paymentRecordService.getPaymentsForSubscription(USER_ID, SUBSCRIPTION_ID, 1, 20))
                    .thenReturn(paginatedResponse);

            mockMvc.perform(get("/subscriptions/{subscriptionId}/payments", SUBSCRIPTION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].paymentDate").value("2024-01-15"))
                    .andExpect(jsonPath("$.data[1].paymentDate").value("2024-02-15"))
                    .andExpect(jsonPath("$.pagination.total").value(2));
        }
    }

    @Nested
    @DisplayName("POST /payments")
    class CreatePayment {

        @Test
        @DisplayName("should return 201 when payment is created successfully")
        void shouldReturn201WhenPaymentIsCreatedSuccessfully() throws Exception {
            when(paymentRecordService.createPayment(eq(USER_ID), any(CreatePaymentRequest.class)))
                    .thenReturn(paymentResponse);

            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(PAYMENT_ID))
                    .andExpect(jsonPath("$.subscriptionId").value(SUBSCRIPTION_ID))
                    .andExpect(jsonPath("$.amount").value(15.99))
                    .andExpect(jsonPath("$.currencyCode").value("USD"))
                    .andExpect(jsonPath("$.paymentDate").value("2024-01-15"));
        }

        @Test
        @DisplayName("should return 400 when subscription ID is missing")
        void shouldReturn400WhenSubscriptionIdIsMissing() throws Exception {
            CreatePaymentRequest invalidRequest = CreatePaymentRequest.builder()
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .paymentDate(LocalDate.of(2024, 1, 15))
                    .build();

            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when amount is missing")
        void shouldReturn400WhenAmountIsMissing() throws Exception {
            CreatePaymentRequest invalidRequest = CreatePaymentRequest.builder()
                    .subscriptionId(SUBSCRIPTION_ID)
                    .currencyCode("USD")
                    .paymentDate(LocalDate.of(2024, 1, 15))
                    .build();

            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when amount is zero or negative")
        void shouldReturn400WhenAmountIsZeroOrNegative() throws Exception {
            CreatePaymentRequest invalidRequest = CreatePaymentRequest.builder()
                    .subscriptionId(SUBSCRIPTION_ID)
                    .amount(new BigDecimal("0.00"))
                    .currencyCode("USD")
                    .paymentDate(LocalDate.of(2024, 1, 15))
                    .build();

            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when currency code is invalid length")
        void shouldReturn400WhenCurrencyCodeIsInvalidLength() throws Exception {
            CreatePaymentRequest invalidRequest = CreatePaymentRequest.builder()
                    .subscriptionId(SUBSCRIPTION_ID)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("US")
                    .paymentDate(LocalDate.of(2024, 1, 15))
                    .build();

            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when currency code is blank")
        void shouldReturn400WhenCurrencyCodeIsBlank() throws Exception {
            CreatePaymentRequest invalidRequest = CreatePaymentRequest.builder()
                    .subscriptionId(SUBSCRIPTION_ID)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("")
                    .paymentDate(LocalDate.of(2024, 1, 15))
                    .build();

            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when payment date is missing")
        void shouldReturn400WhenPaymentDateIsMissing() throws Exception {
            CreatePaymentRequest invalidRequest = CreatePaymentRequest.builder()
                    .subscriptionId(SUBSCRIPTION_ID)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .build();

            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 404 when subscription not found")
        void shouldReturn404WhenSubscriptionNotFound() throws Exception {
            when(paymentRecordService.createPayment(eq(USER_ID), any(CreatePaymentRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Subscription", "id", SUBSCRIPTION_ID));

            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 201 when created without FX rate")
        void shouldReturn201WhenCreatedWithoutFxRate() throws Exception {
            CreatePaymentRequest requestWithoutFx = CreatePaymentRequest.builder()
                    .subscriptionId(SUBSCRIPTION_ID)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .paymentDate(LocalDate.of(2024, 1, 15))
                    .build();

            when(paymentRecordService.createPayment(eq(USER_ID), any(CreatePaymentRequest.class)))
                    .thenReturn(paymentResponse);

            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithoutFx)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(PAYMENT_ID));
        }
    }

    @Nested
    @DisplayName("PUT /payments/{id}")
    class UpdatePayment {

        @Test
        @DisplayName("should return 200 when payment is updated successfully")
        void shouldReturn200WhenPaymentIsUpdatedSuccessfully() throws Exception {
            PaymentRecordResponse updatedResponse = PaymentRecordResponse.builder()
                    .id(PAYMENT_ID)
                    .subscriptionId(SUBSCRIPTION_ID)
                    .amount(new BigDecimal("19.99"))
                    .currencyCode("EUR")
                    .paymentDate(LocalDate.of(2024, 1, 20))
                    .fxRateToBase(new BigDecimal("0.920000"))
                    .amountInBaseCurrency(new BigDecimal("18.39"))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(paymentRecordService.updatePayment(eq(USER_ID), eq(PAYMENT_ID), any(UpdatePaymentRequest.class)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/payments/{id}", PAYMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PAYMENT_ID))
                    .andExpect(jsonPath("$.amount").value(19.99))
                    .andExpect(jsonPath("$.currencyCode").value("EUR"))
                    .andExpect(jsonPath("$.paymentDate").value("2024-01-20"));
        }

        @Test
        @DisplayName("should return 404 when payment not found")
        void shouldReturn404WhenPaymentNotFound() throws Exception {
            when(paymentRecordService.updatePayment(eq(USER_ID), eq(999L), any(UpdatePaymentRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Payment", "id", 999L));

            mockMvc.perform(put("/payments/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 400 when amount is invalid")
        void shouldReturn400WhenAmountIsInvalid() throws Exception {
            UpdatePaymentRequest invalidRequest = UpdatePaymentRequest.builder()
                    .amount(new BigDecimal("-5.00"))
                    .build();

            mockMvc.perform(put("/payments/{id}", PAYMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when currency code length is invalid")
        void shouldReturn400WhenCurrencyCodeLengthIsInvalid() throws Exception {
            UpdatePaymentRequest invalidRequest = UpdatePaymentRequest.builder()
                    .currencyCode("EURO")
                    .build();

            mockMvc.perform(put("/payments/{id}", PAYMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 200 with partial update")
        void shouldReturn200WithPartialUpdate() throws Exception {
            UpdatePaymentRequest partialRequest = UpdatePaymentRequest.builder()
                    .amount(new BigDecimal("25.00"))
                    .build();

            PaymentRecordResponse partiallyUpdated = PaymentRecordResponse.builder()
                    .id(PAYMENT_ID)
                    .subscriptionId(SUBSCRIPTION_ID)
                    .amount(new BigDecimal("25.00"))
                    .currencyCode("USD")
                    .paymentDate(LocalDate.of(2024, 1, 15))
                    .fxRateToBase(new BigDecimal("1.000000"))
                    .amountInBaseCurrency(new BigDecimal("25.00"))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(paymentRecordService.updatePayment(eq(USER_ID), eq(PAYMENT_ID), any(UpdatePaymentRequest.class)))
                    .thenReturn(partiallyUpdated);

            mockMvc.perform(put("/payments/{id}", PAYMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(partialRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.amount").value(25.00))
                    .andExpect(jsonPath("$.currencyCode").value("USD"));
        }
    }

    @Nested
    @DisplayName("DELETE /payments/{id}")
    class DeletePayment {

        @Test
        @DisplayName("should return 204 when payment is deleted successfully")
        void shouldReturn204WhenPaymentIsDeletedSuccessfully() throws Exception {
            doNothing().when(paymentRecordService).deletePayment(USER_ID, PAYMENT_ID);

            mockMvc.perform(delete("/payments/{id}", PAYMENT_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when payment not found")
        void shouldReturn404WhenPaymentNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Payment", "id", 999L))
                    .when(paymentRecordService).deletePayment(USER_ID, 999L);

            mockMvc.perform(delete("/payments/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /fx-rates/refresh")
    class RefreshFxRates {

        @Test
        @DisplayName("should return 200 when FX rates are refreshed successfully")
        void shouldReturn200WhenFxRatesAreRefreshedSuccessfully() throws Exception {
            Map<String, BigDecimal> rates = Map.of(
                    "EUR", new BigDecimal("0.920000"),
                    "GBP", new BigDecimal("0.790000"),
                    "JPY", new BigDecimal("148.500000")
            );

            when(fxRateService.refreshRates()).thenReturn(rates);
            when(jobRunService.recordSuccess(anyString(), any(), any(), any())).thenReturn(null);

            mockMvc.perform(post("/fx-rates/refresh"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("FX rates refreshed successfully"))
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(jsonPath("$.rates").exists())
                    .andExpect(jsonPath("$.rates.EUR").value(0.920000))
                    .andExpect(jsonPath("$.rates.GBP").value(0.790000))
                    .andExpect(jsonPath("$.rates.JPY").value(148.500000));
        }

        @Test
        @DisplayName("should return 500 when FX rate refresh fails")
        void shouldReturn500WhenFxRateRefreshFails() throws Exception {
            when(fxRateService.refreshRates())
                    .thenThrow(new RuntimeException("Failed to fetch FX rates from external API"));
            when(jobRunService.recordFailure(anyString(), any(), any(), any(), anyString())).thenReturn(null);

            mockMvc.perform(post("/fx-rates/refresh"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("should return 200 with empty rates map")
        void shouldReturn200WithEmptyRatesMap() throws Exception {
            when(fxRateService.refreshRates()).thenReturn(Map.of());
            when(jobRunService.recordSuccess(anyString(), any(), any(), any())).thenReturn(null);

            mockMvc.perform(post("/fx-rates/refresh"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("FX rates refreshed successfully"))
                    .andExpect(jsonPath("$.rates").isEmpty());
        }
    }
}
