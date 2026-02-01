package com.subscriptiontracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptiontracker.dto.request.CancelSubscriptionRequest;
import com.subscriptiontracker.dto.request.CreateSubscriptionRequest;
import com.subscriptiontracker.dto.request.ReactivateSubscriptionRequest;
import com.subscriptiontracker.dto.request.UpdateSubscriptionRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.ServiceResponse;
import com.subscriptiontracker.dto.response.SubscriptionDetailResponse;
import com.subscriptiontracker.dto.response.SubscriptionResponse;
import com.subscriptiontracker.entity.BillingCycle;
import com.subscriptiontracker.entity.SubscriptionStatus;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.JwtService;
import com.subscriptiontracker.service.SubscriptionService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link SubscriptionController}.
 *
 * <p>Tests all subscription management endpoints including CRUD operations,
 * cancellation, reactivation, and category retrieval.</p>
 *
 * @author Generated
 * @since 1.0
 */
@WebMvcTest(SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SubscriptionController")
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriptionService subscriptionService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private CurrentUserService currentUserService;

    private static final Long USER_ID = 1L;
    private static final Long SUBSCRIPTION_ID = 100L;

    private ServiceResponse serviceResponse;
    private SubscriptionResponse subscriptionResponse;
    private SubscriptionDetailResponse subscriptionDetailResponse;
    private CreateSubscriptionRequest validCreateRequest;
    private UpdateSubscriptionRequest validUpdateRequest;
    private CancelSubscriptionRequest validCancelRequest;
    private ReactivateSubscriptionRequest validReactivateRequest;

    @BeforeEach
    void setUp() {
        when(currentUserService.getCurrentUserId(any())).thenReturn(USER_ID);

        serviceResponse = ServiceResponse.builder()
                .id(1L)
                .name("Netflix")
                .category("Entertainment")
                .websiteUrl("https://netflix.com")
                .subscriptionCount(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        subscriptionResponse = SubscriptionResponse.builder()
                .id(SUBSCRIPTION_ID)
                .service(serviceResponse)
                .amount(new BigDecimal("15.99"))
                .currencyCode("USD")
                .billingCycle(BillingCycle.monthly)
                .paymentMethod("Credit Card")
                .startDate(LocalDate.of(2024, 1, 15))
                .nextBillingDate(LocalDate.of(2024, 2, 15))
                .status(SubscriptionStatus.active)
                .notes("Premium plan")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        SubscriptionDetailResponse.Stats stats = SubscriptionDetailResponse.Stats.builder()
                .totalPaid(new BigDecimal("47.97"))
                .totalPayments(3)
                .averagePerMonth(new BigDecimal("15.99"))
                .activeSince(LocalDate.of(2024, 1, 15))
                .build();

        subscriptionDetailResponse = SubscriptionDetailResponse.builder()
                .id(SUBSCRIPTION_ID)
                .service(serviceResponse)
                .amount(new BigDecimal("15.99"))
                .currencyCode("USD")
                .billingCycle(BillingCycle.monthly)
                .paymentMethod("Credit Card")
                .startDate(LocalDate.of(2024, 1, 15))
                .nextBillingDate(LocalDate.of(2024, 2, 15))
                .status(SubscriptionStatus.active)
                .notes("Premium plan")
                .stats(stats)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validCreateRequest = CreateSubscriptionRequest.builder()
                .serviceId(1L)
                .amount(new BigDecimal("15.99"))
                .currencyCode("USD")
                .billingCycle(BillingCycle.monthly)
                .startDate(LocalDate.of(2024, 1, 15))
                .nextBillingDate(LocalDate.of(2024, 2, 15))
                .paymentMethod("Credit Card")
                .notes("Premium plan")
                .build();

        validUpdateRequest = UpdateSubscriptionRequest.builder()
                .amount(new BigDecimal("19.99"))
                .currencyCode("EUR")
                .build();

        validCancelRequest = CancelSubscriptionRequest.builder()
                .cancelledAt(LocalDate.of(2024, 3, 1))
                .build();

        validReactivateRequest = ReactivateSubscriptionRequest.builder()
                .nextBillingDate(LocalDate.of(2024, 4, 1))
                .build();
    }

    @Nested
    @DisplayName("GET /subscriptions")
    class GetSubscriptions {

        @Test
        @DisplayName("should return 200 with paginated subscriptions")
        void shouldReturn200WithPaginatedSubscriptions() throws Exception {
            PaginatedResponse<SubscriptionResponse> paginatedResponse = PaginatedResponse.of(
                    List.of(subscriptionResponse), 1, 20, 1
            );

            when(subscriptionService.getSubscriptions(
                    eq(USER_ID), isNull(), isNull(), isNull(),
                    eq("nextBillingDate"), eq("asc"), eq(1), eq(20)
            )).thenReturn(paginatedResponse);

            mockMvc.perform(get("/subscriptions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(SUBSCRIPTION_ID))
                    .andExpect(jsonPath("$.data[0].service.name").value("Netflix"))
                    .andExpect(jsonPath("$.data[0].amount").value(15.99))
                    .andExpect(jsonPath("$.data[0].currencyCode").value("USD"))
                    .andExpect(jsonPath("$.data[0].billingCycle").value("monthly"))
                    .andExpect(jsonPath("$.data[0].status").value("active"))
                    .andExpect(jsonPath("$.pagination.page").value(1))
                    .andExpect(jsonPath("$.pagination.limit").value(20))
                    .andExpect(jsonPath("$.pagination.total").value(1));
        }

        @Test
        @DisplayName("should return 200 with filtered subscriptions by status")
        void shouldReturn200WithFilteredSubscriptionsByStatus() throws Exception {
            PaginatedResponse<SubscriptionResponse> paginatedResponse = PaginatedResponse.of(
                    List.of(subscriptionResponse), 1, 20, 1
            );

            when(subscriptionService.getSubscriptions(
                    eq(USER_ID), eq(SubscriptionStatus.active), isNull(), isNull(),
                    eq("nextBillingDate"), eq("asc"), eq(1), eq(20)
            )).thenReturn(paginatedResponse);

            mockMvc.perform(get("/subscriptions")
                            .param("status", "active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("should return 200 with custom pagination")
        void shouldReturn200WithCustomPagination() throws Exception {
            PaginatedResponse<SubscriptionResponse> paginatedResponse = PaginatedResponse.of(
                    List.of(subscriptionResponse), 2, 10, 15
            );

            when(subscriptionService.getSubscriptions(
                    eq(USER_ID), isNull(), isNull(), isNull(),
                    eq("amount"), eq("desc"), eq(2), eq(10)
            )).thenReturn(paginatedResponse);

            mockMvc.perform(get("/subscriptions")
                            .param("page", "2")
                            .param("limit", "10")
                            .param("sort", "amount")
                            .param("order", "desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pagination.page").value(2))
                    .andExpect(jsonPath("$.pagination.limit").value(10));
        }

        @Test
        @DisplayName("should return 200 with empty list when no subscriptions")
        void shouldReturn200WithEmptyListWhenNoSubscriptions() throws Exception {
            PaginatedResponse<SubscriptionResponse> emptyResponse = PaginatedResponse.of(
                    List.of(), 1, 20, 0
            );

            when(subscriptionService.getSubscriptions(
                    eq(USER_ID), isNull(), isNull(), isNull(),
                    eq("nextBillingDate"), eq("asc"), eq(1), eq(20)
            )).thenReturn(emptyResponse);

            mockMvc.perform(get("/subscriptions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.pagination.total").value(0));
        }
    }

    @Nested
    @DisplayName("GET /subscriptions/{id}")
    class GetSubscriptionById {

        @Test
        @DisplayName("should return 200 with subscription details")
        void shouldReturn200WithSubscriptionDetails() throws Exception {
            when(subscriptionService.getSubscriptionDetail(USER_ID, SUBSCRIPTION_ID))
                    .thenReturn(subscriptionDetailResponse);

            mockMvc.perform(get("/subscriptions/{id}", SUBSCRIPTION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID))
                    .andExpect(jsonPath("$.service.name").value("Netflix"))
                    .andExpect(jsonPath("$.amount").value(15.99))
                    .andExpect(jsonPath("$.currencyCode").value("USD"))
                    .andExpect(jsonPath("$.billingCycle").value("monthly"))
                    .andExpect(jsonPath("$.status").value("active"))
                    .andExpect(jsonPath("$.stats.totalPaid").value(47.97))
                    .andExpect(jsonPath("$.stats.totalPayments").value(3))
                    .andExpect(jsonPath("$.stats.averagePerMonth").value(15.99));
        }

        @Test
        @DisplayName("should return 404 when subscription not found")
        void shouldReturn404WhenSubscriptionNotFound() throws Exception {
            when(subscriptionService.getSubscriptionDetail(USER_ID, 999L))
                    .thenThrow(new ResourceNotFoundException("Subscription", "id", 999L));

            mockMvc.perform(get("/subscriptions/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /subscriptions")
    class CreateSubscription {

        @Test
        @DisplayName("should return 201 when subscription is created successfully")
        void shouldReturn201WhenSubscriptionIsCreatedSuccessfully() throws Exception {
            when(subscriptionService.createSubscription(eq(USER_ID), any(CreateSubscriptionRequest.class)))
                    .thenReturn(subscriptionResponse);

            mockMvc.perform(post("/subscriptions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID))
                    .andExpect(jsonPath("$.service.name").value("Netflix"))
                    .andExpect(jsonPath("$.amount").value(15.99))
                    .andExpect(jsonPath("$.currencyCode").value("USD"));
        }

        @Test
        @DisplayName("should return 400 when service ID is missing")
        void shouldReturn400WhenServiceIdIsMissing() throws Exception {
            CreateSubscriptionRequest invalidRequest = CreateSubscriptionRequest.builder()
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .startDate(LocalDate.of(2024, 1, 15))
                    .nextBillingDate(LocalDate.of(2024, 2, 15))
                    .build();

            mockMvc.perform(post("/subscriptions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when amount is missing")
        void shouldReturn400WhenAmountIsMissing() throws Exception {
            CreateSubscriptionRequest invalidRequest = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .startDate(LocalDate.of(2024, 1, 15))
                    .nextBillingDate(LocalDate.of(2024, 2, 15))
                    .build();

            mockMvc.perform(post("/subscriptions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when amount is zero or negative")
        void shouldReturn400WhenAmountIsZeroOrNegative() throws Exception {
            CreateSubscriptionRequest invalidRequest = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("0.00"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .startDate(LocalDate.of(2024, 1, 15))
                    .nextBillingDate(LocalDate.of(2024, 2, 15))
                    .build();

            mockMvc.perform(post("/subscriptions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when currency code is invalid length")
        void shouldReturn400WhenCurrencyCodeIsInvalidLength() throws Exception {
            CreateSubscriptionRequest invalidRequest = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("US")
                    .billingCycle(BillingCycle.monthly)
                    .startDate(LocalDate.of(2024, 1, 15))
                    .nextBillingDate(LocalDate.of(2024, 2, 15))
                    .build();

            mockMvc.perform(post("/subscriptions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when billing cycle is missing")
        void shouldReturn400WhenBillingCycleIsMissing() throws Exception {
            CreateSubscriptionRequest invalidRequest = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .startDate(LocalDate.of(2024, 1, 15))
                    .nextBillingDate(LocalDate.of(2024, 2, 15))
                    .build();

            mockMvc.perform(post("/subscriptions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 201 when start date is missing (auto-calculated)")
        void shouldReturn201WhenStartDateIsMissing() throws Exception {
            // Start date is now optional - it's auto-calculated from nextBillingDate and billingCycle
            CreateSubscriptionRequest requestWithoutStartDate = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .nextBillingDate(LocalDate.of(2024, 2, 15))
                    .build();

            when(subscriptionService.createSubscription(eq(USER_ID), any(CreateSubscriptionRequest.class)))
                    .thenReturn(subscriptionResponse);

            mockMvc.perform(post("/subscriptions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithoutStartDate)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID));
        }

        @Test
        @DisplayName("should return 404 when service not found")
        void shouldReturn404WhenServiceNotFound() throws Exception {
            when(subscriptionService.createSubscription(eq(USER_ID), any(CreateSubscriptionRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Service", "id", 1L));

            mockMvc.perform(post("/subscriptions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 400 when custom billing cycle lacks days")
        void shouldReturn400WhenCustomBillingCycleLacksDays() throws Exception {
            CreateSubscriptionRequest customRequest = CreateSubscriptionRequest.builder()
                    .serviceId(1L)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.custom)
                    .startDate(LocalDate.of(2024, 1, 15))
                    .nextBillingDate(LocalDate.of(2024, 2, 15))
                    .build();

            when(subscriptionService.createSubscription(eq(USER_ID), any(CreateSubscriptionRequest.class)))
                    .thenThrow(new BadRequestException("Billing cycle days is required for custom billing cycle"));

            mockMvc.perform(post("/subscriptions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(customRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }
    }

    @Nested
    @DisplayName("PUT /subscriptions/{id}")
    class UpdateSubscription {

        @Test
        @DisplayName("should return 200 when subscription is updated successfully")
        void shouldReturn200WhenSubscriptionIsUpdatedSuccessfully() throws Exception {
            SubscriptionResponse updatedResponse = SubscriptionResponse.builder()
                    .id(SUBSCRIPTION_ID)
                    .service(serviceResponse)
                    .amount(new BigDecimal("19.99"))
                    .currencyCode("EUR")
                    .billingCycle(BillingCycle.monthly)
                    .status(SubscriptionStatus.active)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(subscriptionService.updateSubscription(eq(USER_ID), eq(SUBSCRIPTION_ID), any(UpdateSubscriptionRequest.class)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/subscriptions/{id}", SUBSCRIPTION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID))
                    .andExpect(jsonPath("$.amount").value(19.99))
                    .andExpect(jsonPath("$.currencyCode").value("EUR"));
        }

        @Test
        @DisplayName("should return 404 when subscription not found")
        void shouldReturn404WhenSubscriptionNotFound() throws Exception {
            when(subscriptionService.updateSubscription(eq(USER_ID), eq(999L), any(UpdateSubscriptionRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Subscription", "id", 999L));

            mockMvc.perform(put("/subscriptions/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 400 when amount is invalid")
        void shouldReturn400WhenAmountIsInvalid() throws Exception {
            UpdateSubscriptionRequest invalidRequest = UpdateSubscriptionRequest.builder()
                    .amount(new BigDecimal("-5.00"))
                    .build();

            mockMvc.perform(put("/subscriptions/{id}", SUBSCRIPTION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when currency code length is invalid")
        void shouldReturn400WhenCurrencyCodeLengthIsInvalid() throws Exception {
            UpdateSubscriptionRequest invalidRequest = UpdateSubscriptionRequest.builder()
                    .currencyCode("EURO")
                    .build();

            mockMvc.perform(put("/subscriptions/{id}", SUBSCRIPTION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("PATCH /subscriptions/{id}/cancel")
    class CancelSubscription {

        @Test
        @DisplayName("should return 200 when subscription is cancelled successfully")
        void shouldReturn200WhenSubscriptionIsCancelledSuccessfully() throws Exception {
            SubscriptionResponse cancelledResponse = SubscriptionResponse.builder()
                    .id(SUBSCRIPTION_ID)
                    .service(serviceResponse)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .status(SubscriptionStatus.cancelled)
                    .cancelledAt(LocalDateTime.of(2024, 3, 1, 0, 0))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(subscriptionService.cancelSubscription(eq(USER_ID), eq(SUBSCRIPTION_ID), any(CancelSubscriptionRequest.class)))
                    .thenReturn(cancelledResponse);

            mockMvc.perform(patch("/subscriptions/{id}/cancel", SUBSCRIPTION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCancelRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID))
                    .andExpect(jsonPath("$.status").value("cancelled"));
        }

        @Test
        @DisplayName("should return 404 when subscription not found")
        void shouldReturn404WhenSubscriptionNotFound() throws Exception {
            when(subscriptionService.cancelSubscription(eq(USER_ID), eq(999L), any(CancelSubscriptionRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Subscription", "id", 999L));

            mockMvc.perform(patch("/subscriptions/{id}/cancel", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCancelRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 400 when subscription is already cancelled")
        void shouldReturn400WhenSubscriptionIsAlreadyCancelled() throws Exception {
            when(subscriptionService.cancelSubscription(eq(USER_ID), eq(SUBSCRIPTION_ID), any(CancelSubscriptionRequest.class)))
                    .thenThrow(new BadRequestException("Subscription is already cancelled"));

            mockMvc.perform(patch("/subscriptions/{id}/cancel", SUBSCRIPTION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCancelRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("should return 400 when cancelled date is missing")
        void shouldReturn400WhenCancelledDateIsMissing() throws Exception {
            CancelSubscriptionRequest invalidRequest = CancelSubscriptionRequest.builder().build();

            mockMvc.perform(patch("/subscriptions/{id}/cancel", SUBSCRIPTION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("PATCH /subscriptions/{id}/reactivate")
    class ReactivateSubscription {

        @Test
        @DisplayName("should return 200 when subscription is reactivated successfully")
        void shouldReturn200WhenSubscriptionIsReactivatedSuccessfully() throws Exception {
            SubscriptionResponse reactivatedResponse = SubscriptionResponse.builder()
                    .id(SUBSCRIPTION_ID)
                    .service(serviceResponse)
                    .amount(new BigDecimal("15.99"))
                    .currencyCode("USD")
                    .billingCycle(BillingCycle.monthly)
                    .status(SubscriptionStatus.active)
                    .nextBillingDate(LocalDate.of(2024, 4, 1))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(subscriptionService.reactivateSubscription(eq(USER_ID), eq(SUBSCRIPTION_ID), any(ReactivateSubscriptionRequest.class)))
                    .thenReturn(reactivatedResponse);

            mockMvc.perform(patch("/subscriptions/{id}/reactivate", SUBSCRIPTION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validReactivateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID))
                    .andExpect(jsonPath("$.status").value("active"));
        }

        @Test
        @DisplayName("should return 404 when subscription not found")
        void shouldReturn404WhenSubscriptionNotFound() throws Exception {
            when(subscriptionService.reactivateSubscription(eq(USER_ID), eq(999L), any(ReactivateSubscriptionRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Subscription", "id", 999L));

            mockMvc.perform(patch("/subscriptions/{id}/reactivate", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validReactivateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 400 when subscription is already active")
        void shouldReturn400WhenSubscriptionIsAlreadyActive() throws Exception {
            when(subscriptionService.reactivateSubscription(eq(USER_ID), eq(SUBSCRIPTION_ID), any(ReactivateSubscriptionRequest.class)))
                    .thenThrow(new BadRequestException("Subscription is already active"));

            mockMvc.perform(patch("/subscriptions/{id}/reactivate", SUBSCRIPTION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validReactivateRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("should return 400 when next billing date is missing")
        void shouldReturn400WhenNextBillingDateIsMissing() throws Exception {
            ReactivateSubscriptionRequest invalidRequest = ReactivateSubscriptionRequest.builder().build();

            mockMvc.perform(patch("/subscriptions/{id}/reactivate", SUBSCRIPTION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("DELETE /subscriptions/{id}")
    class DeleteSubscription {

        @Test
        @DisplayName("should return 204 when subscription is deleted successfully")
        void shouldReturn204WhenSubscriptionIsDeletedSuccessfully() throws Exception {
            doNothing().when(subscriptionService).deleteSubscription(USER_ID, SUBSCRIPTION_ID);

            mockMvc.perform(delete("/subscriptions/{id}", SUBSCRIPTION_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when subscription not found")
        void shouldReturn404WhenSubscriptionNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Subscription", "id", 999L))
                    .when(subscriptionService).deleteSubscription(USER_ID, 999L);

            mockMvc.perform(delete("/subscriptions/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("GET /subscriptions/categories")
    class GetCategories {

        @Test
        @DisplayName("should return 200 with list of categories")
        void shouldReturn200WithListOfCategories() throws Exception {
            List<String> categories = List.of("Entertainment", "Software", "Health");

            when(subscriptionService.getCategories(USER_ID)).thenReturn(categories);

            mockMvc.perform(get("/subscriptions/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0]").value("Entertainment"))
                    .andExpect(jsonPath("$[1]").value("Software"))
                    .andExpect(jsonPath("$[2]").value("Health"));
        }

        @Test
        @DisplayName("should return 200 with empty list when no categories exist")
        void shouldReturn200WithEmptyListWhenNoCategoriesExist() throws Exception {
            when(subscriptionService.getCategories(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/subscriptions/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }
}
