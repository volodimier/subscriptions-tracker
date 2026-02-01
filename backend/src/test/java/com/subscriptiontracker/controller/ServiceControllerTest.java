package com.subscriptiontracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptiontracker.dto.request.CreateServiceRequest;
import com.subscriptiontracker.dto.request.UpdateServiceRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.ServiceResponse;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.JwtService;
import com.subscriptiontracker.service.ServiceService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link ServiceController}.
 *
 * <p>Tests all service catalog management endpoints including CRUD operations
 * and category retrieval.</p>
 *
 * @author Generated
 * @since 1.0
 */
@WebMvcTest(ServiceController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ServiceController")
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ServiceService serviceService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private CurrentUserService currentUserService;

    private static final Long USER_ID = 1L;
    private static final Long SERVICE_ID = 100L;

    private ServiceResponse serviceResponse;
    private CreateServiceRequest validCreateRequest;
    private UpdateServiceRequest validUpdateRequest;

    @BeforeEach
    void setUp() {
        when(currentUserService.getCurrentUserId(any())).thenReturn(USER_ID);

        serviceResponse = ServiceResponse.builder()
                .id(SERVICE_ID)
                .name("Netflix")
                .category("Entertainment")
                .websiteUrl("https://netflix.com")
                .subscriptionCount(5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validCreateRequest = CreateServiceRequest.builder()
                .name("Spotify")
                .category("Music")
                .websiteUrl("https://spotify.com")
                .build();

        validUpdateRequest = UpdateServiceRequest.builder()
                .name("Spotify Premium")
                .category("Music Streaming")
                .build();
    }

    @Nested
    @DisplayName("GET /services")
    class GetAllServices {

        @Test
        @DisplayName("should return 200 with paginated services")
        void shouldReturn200WithPaginatedServices() throws Exception {
            PaginatedResponse<ServiceResponse> paginatedResponse = PaginatedResponse.of(
                    List.of(serviceResponse), 1, 20, 1
            );

            when(serviceService.getAllServices(eq(USER_ID), isNull(), eq(1), eq(20)))
                    .thenReturn(paginatedResponse);

            mockMvc.perform(get("/services"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(SERVICE_ID))
                    .andExpect(jsonPath("$.data[0].name").value("Netflix"))
                    .andExpect(jsonPath("$.data[0].category").value("Entertainment"))
                    .andExpect(jsonPath("$.data[0].websiteUrl").value("https://netflix.com"))
                    .andExpect(jsonPath("$.data[0].subscriptionCount").value(5))
                    .andExpect(jsonPath("$.pagination.page").value(1))
                    .andExpect(jsonPath("$.pagination.limit").value(20))
                    .andExpect(jsonPath("$.pagination.total").value(1));
        }

        @Test
        @DisplayName("should return 200 with search results")
        void shouldReturn200WithSearchResults() throws Exception {
            PaginatedResponse<ServiceResponse> paginatedResponse = PaginatedResponse.of(
                    List.of(serviceResponse), 1, 20, 1
            );

            when(serviceService.getAllServices(eq(USER_ID), eq("Netflix"), eq(1), eq(20)))
                    .thenReturn(paginatedResponse);

            mockMvc.perform(get("/services")
                            .param("search", "Netflix"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].name").value("Netflix"));
        }

        @Test
        @DisplayName("should return 200 with custom pagination")
        void shouldReturn200WithCustomPagination() throws Exception {
            PaginatedResponse<ServiceResponse> paginatedResponse = PaginatedResponse.of(
                    List.of(serviceResponse), 2, 10, 15
            );

            when(serviceService.getAllServices(eq(USER_ID), isNull(), eq(2), eq(10)))
                    .thenReturn(paginatedResponse);

            mockMvc.perform(get("/services")
                            .param("page", "2")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pagination.page").value(2))
                    .andExpect(jsonPath("$.pagination.limit").value(10));
        }

        @Test
        @DisplayName("should return 200 with empty list when no services")
        void shouldReturn200WithEmptyListWhenNoServices() throws Exception {
            PaginatedResponse<ServiceResponse> emptyResponse = PaginatedResponse.of(
                    List.of(), 1, 20, 0
            );

            when(serviceService.getAllServices(eq(USER_ID), isNull(), eq(1), eq(20)))
                    .thenReturn(emptyResponse);

            mockMvc.perform(get("/services"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.pagination.total").value(0));
        }
    }

    @Nested
    @DisplayName("GET /services/all")
    class GetAllServicesForDropdown {

        @Test
        @DisplayName("should return 200 with list of all services")
        void shouldReturn200WithListOfAllServices() throws Exception {
            ServiceResponse service2 = ServiceResponse.builder()
                    .id(101L)
                    .name("Spotify")
                    .category("Music")
                    .websiteUrl("https://spotify.com")
                    .subscriptionCount(3)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(serviceService.getAllServicesForUser(USER_ID))
                    .thenReturn(List.of(serviceResponse, service2));

            mockMvc.perform(get("/services/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(SERVICE_ID))
                    .andExpect(jsonPath("$[0].name").value("Netflix"))
                    .andExpect(jsonPath("$[1].id").value(101))
                    .andExpect(jsonPath("$[1].name").value("Spotify"));
        }

        @Test
        @DisplayName("should return 200 with empty list when no services")
        void shouldReturn200WithEmptyListWhenNoServices() throws Exception {
            when(serviceService.getAllServicesForUser(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/services/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /services/{id}")
    class GetServiceById {

        @Test
        @DisplayName("should return 200 with service details")
        void shouldReturn200WithServiceDetails() throws Exception {
            when(serviceService.getService(USER_ID, SERVICE_ID)).thenReturn(serviceResponse);

            mockMvc.perform(get("/services/{id}", SERVICE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SERVICE_ID))
                    .andExpect(jsonPath("$.name").value("Netflix"))
                    .andExpect(jsonPath("$.category").value("Entertainment"))
                    .andExpect(jsonPath("$.websiteUrl").value("https://netflix.com"))
                    .andExpect(jsonPath("$.subscriptionCount").value(5));
        }

        @Test
        @DisplayName("should return 404 when service not found")
        void shouldReturn404WhenServiceNotFound() throws Exception {
            when(serviceService.getService(USER_ID, 999L))
                    .thenThrow(new ResourceNotFoundException("Service", "id", 999L));

            mockMvc.perform(get("/services/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /services")
    class CreateService {

        @Test
        @DisplayName("should return 201 when service is created successfully")
        void shouldReturn201WhenServiceIsCreatedSuccessfully() throws Exception {
            ServiceResponse createdResponse = ServiceResponse.builder()
                    .id(101L)
                    .name("Spotify")
                    .category("Music")
                    .websiteUrl("https://spotify.com")
                    .subscriptionCount(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(serviceService.createService(eq(USER_ID), any(CreateServiceRequest.class)))
                    .thenReturn(createdResponse);

            mockMvc.perform(post("/services")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(101))
                    .andExpect(jsonPath("$.name").value("Spotify"))
                    .andExpect(jsonPath("$.category").value("Music"))
                    .andExpect(jsonPath("$.websiteUrl").value("https://spotify.com"));
        }

        @Test
        @DisplayName("should return 400 when service name is blank")
        void shouldReturn400WhenServiceNameIsBlank() throws Exception {
            CreateServiceRequest invalidRequest = CreateServiceRequest.builder()
                    .name("")
                    .category("Music")
                    .build();

            mockMvc.perform(post("/services")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when service name is missing")
        void shouldReturn400WhenServiceNameIsMissing() throws Exception {
            CreateServiceRequest invalidRequest = CreateServiceRequest.builder()
                    .category("Music")
                    .build();

            mockMvc.perform(post("/services")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when service name exceeds max length")
        void shouldReturn400WhenServiceNameExceedsMaxLength() throws Exception {
            String longName = "A".repeat(256);
            CreateServiceRequest invalidRequest = CreateServiceRequest.builder()
                    .name(longName)
                    .category("Music")
                    .build();

            mockMvc.perform(post("/services")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when category exceeds max length")
        void shouldReturn400WhenCategoryExceedsMaxLength() throws Exception {
            String longCategory = "A".repeat(101);
            CreateServiceRequest invalidRequest = CreateServiceRequest.builder()
                    .name("Spotify")
                    .category(longCategory)
                    .build();

            mockMvc.perform(post("/services")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when website URL exceeds max length")
        void shouldReturn400WhenWebsiteUrlExceedsMaxLength() throws Exception {
            String longUrl = "https://" + "a".repeat(493);
            CreateServiceRequest invalidRequest = CreateServiceRequest.builder()
                    .name("Spotify")
                    .websiteUrl(longUrl)
                    .build();

            mockMvc.perform(post("/services")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 201 when only name is provided")
        void shouldReturn201WhenOnlyNameIsProvided() throws Exception {
            CreateServiceRequest minimalRequest = CreateServiceRequest.builder()
                    .name("Custom Service")
                    .build();

            ServiceResponse createdResponse = ServiceResponse.builder()
                    .id(102L)
                    .name("Custom Service")
                    .subscriptionCount(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(serviceService.createService(eq(USER_ID), any(CreateServiceRequest.class)))
                    .thenReturn(createdResponse);

            mockMvc.perform(post("/services")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(minimalRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(102))
                    .andExpect(jsonPath("$.name").value("Custom Service"));
        }
    }

    @Nested
    @DisplayName("PUT /services/{id}")
    class UpdateService {

        @Test
        @DisplayName("should return 200 when service is updated successfully")
        void shouldReturn200WhenServiceIsUpdatedSuccessfully() throws Exception {
            ServiceResponse updatedResponse = ServiceResponse.builder()
                    .id(SERVICE_ID)
                    .name("Spotify Premium")
                    .category("Music Streaming")
                    .websiteUrl("https://netflix.com")
                    .subscriptionCount(5)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(serviceService.updateService(eq(USER_ID), eq(SERVICE_ID), any(UpdateServiceRequest.class)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/services/{id}", SERVICE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SERVICE_ID))
                    .andExpect(jsonPath("$.name").value("Spotify Premium"))
                    .andExpect(jsonPath("$.category").value("Music Streaming"));
        }

        @Test
        @DisplayName("should return 404 when service not found")
        void shouldReturn404WhenServiceNotFound() throws Exception {
            when(serviceService.updateService(eq(USER_ID), eq(999L), any(UpdateServiceRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Service", "id", 999L));

            mockMvc.perform(put("/services/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 400 when trying to update global service")
        void shouldReturn400WhenTryingToUpdateGlobalService() throws Exception {
            when(serviceService.updateService(eq(USER_ID), eq(SERVICE_ID), any(UpdateServiceRequest.class)))
                    .thenThrow(new BadRequestException("Cannot modify global service"));

            mockMvc.perform(put("/services/{id}", SERVICE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("should return 400 when service name exceeds max length")
        void shouldReturn400WhenServiceNameExceedsMaxLength() throws Exception {
            String longName = "A".repeat(256);
            UpdateServiceRequest invalidRequest = UpdateServiceRequest.builder()
                    .name(longName)
                    .build();

            mockMvc.perform(put("/services/{id}", SERVICE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("DELETE /services/{id}")
    class DeleteService {

        @Test
        @DisplayName("should return 204 when service is deleted successfully")
        void shouldReturn204WhenServiceIsDeletedSuccessfully() throws Exception {
            doNothing().when(serviceService).deleteService(USER_ID, SERVICE_ID);

            mockMvc.perform(delete("/services/{id}", SERVICE_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when service not found")
        void shouldReturn404WhenServiceNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Service", "id", 999L))
                    .when(serviceService).deleteService(USER_ID, 999L);

            mockMvc.perform(delete("/services/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 400 when trying to delete global service")
        void shouldReturn400WhenTryingToDeleteGlobalService() throws Exception {
            doThrow(new BadRequestException("Cannot delete global service"))
                    .when(serviceService).deleteService(USER_ID, SERVICE_ID);

            mockMvc.perform(delete("/services/{id}", SERVICE_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("should return 400 when service is in use")
        void shouldReturn400WhenServiceIsInUse() throws Exception {
            doThrow(new BadRequestException("Cannot delete service that is in use"))
                    .when(serviceService).deleteService(USER_ID, SERVICE_ID);

            mockMvc.perform(delete("/services/{id}", SERVICE_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }
    }

    @Nested
    @DisplayName("GET /services/categories")
    class GetCategories {

        @Test
        @DisplayName("should return 200 with list of categories")
        void shouldReturn200WithListOfCategories() throws Exception {
            List<String> categories = List.of("Entertainment", "Music", "Software", "Health");

            when(serviceService.getCategories(USER_ID)).thenReturn(categories);

            mockMvc.perform(get("/services/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0]").value("Entertainment"))
                    .andExpect(jsonPath("$[1]").value("Music"))
                    .andExpect(jsonPath("$[2]").value("Software"))
                    .andExpect(jsonPath("$[3]").value("Health"));
        }

        @Test
        @DisplayName("should return 200 with empty list when no categories exist")
        void shouldReturn200WithEmptyListWhenNoCategoriesExist() throws Exception {
            when(serviceService.getCategories(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/services/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }
}
