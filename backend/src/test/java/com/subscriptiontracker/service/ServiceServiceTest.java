package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.request.CreateServiceRequest;
import com.subscriptiontracker.dto.request.UpdateServiceRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.ServiceResponse;
import com.subscriptiontracker.entity.Service;
import com.subscriptiontracker.entity.User;
import com.subscriptiontracker.exception.BadRequestException;
import com.subscriptiontracker.exception.DuplicateResourceException;
import com.subscriptiontracker.exception.ResourceNotFoundException;
import com.subscriptiontracker.repository.ServiceRepository;
import com.subscriptiontracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceService")
class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FaviconService faviconService;

    @InjectMocks
    private ServiceService serviceService;

    private User testUser;
    private Service testService;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .baseCurrencyCode("USD")
                .build();

        testService = Service.builder()
                .id(1L)
                .user(testUser)
                .name("Netflix")
                .category("Entertainment")
                .websiteUrl("https://netflix.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getAllServices")
    class GetAllServices {

        @Test
        @DisplayName("should return paginated services for user")
        void shouldReturnPaginatedServicesForUser() {
            Page<Service> page = new PageImpl<>(List.of(testService));
            when(serviceRepository.findByUserId(anyLong(), any(Pageable.class))).thenReturn(page);
            when(serviceRepository.countSubscriptionsByServiceId(1L)).thenReturn(2L);

            PaginatedResponse<ServiceResponse> result = serviceService.getAllServices(1L, null, 1, 10);

            assertNotNull(result);
            assertEquals(1, result.getData().size());
            assertEquals("Netflix", result.getData().get(0).getName());
            assertEquals(2L, result.getData().get(0).getSubscriptionCount());
        }

        @Test
        @DisplayName("should filter by search term when provided")
        void shouldFilterBySearchTermWhenProvided() {
            Page<Service> page = new PageImpl<>(List.of(testService));
            when(serviceRepository.findByUserIdAndNameContaining(anyLong(), eq("Net"), any(Pageable.class)))
                    .thenReturn(page);
            when(serviceRepository.countSubscriptionsByServiceId(1L)).thenReturn(0L);

            PaginatedResponse<ServiceResponse> result = serviceService.getAllServices(1L, "Net", 1, 10);

            assertNotNull(result);
            verify(serviceRepository).findByUserIdAndNameContaining(eq(1L), eq("Net"), any());
        }
    }

    @Nested
    @DisplayName("getAllServicesForUser")
    class GetAllServicesForUser {

        @Test
        @DisplayName("should return all services for dropdown")
        void shouldReturnAllServicesForDropdown() {
            when(serviceRepository.findByUserIdOrderByNameAsc(1L)).thenReturn(List.of(testService));
            when(serviceRepository.countSubscriptionsByServiceId(1L)).thenReturn(1L);

            List<ServiceResponse> result = serviceService.getAllServicesForUser(1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Netflix", result.get(0).getName());
        }
    }

    @Nested
    @DisplayName("getService")
    class GetService {

        @Test
        @DisplayName("should return service by id")
        void shouldReturnServiceById() {
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(serviceRepository.countSubscriptionsByServiceId(1L)).thenReturn(3L);

            ServiceResponse result = serviceService.getService(1L, 1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Netflix", result.getName());
            assertEquals(3L, result.getSubscriptionCount());
        }

        @Test
        @DisplayName("should throw exception when service not found")
        void shouldThrowExceptionWhenServiceNotFound() {
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    serviceService.getService(1L, 1L)
            );
        }
    }

    @Nested
    @DisplayName("createService")
    class CreateService {

        @Test
        @DisplayName("should create service successfully")
        void shouldCreateServiceSuccessfully() {
            CreateServiceRequest request = CreateServiceRequest.builder()
                    .name("Spotify")
                    .category("Music")
                    .websiteUrl("https://spotify.com")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.existsByUserIdAndName(1L, "Spotify")).thenReturn(false);
            when(faviconService.fetchFavicon("https://spotify.com")).thenReturn(Optional.empty());
            when(serviceRepository.save(any(Service.class))).thenReturn(testService);

            ServiceResponse result = serviceService.createService(1L, request);

            assertNotNull(result);
            verify(serviceRepository).save(any(Service.class));
        }

        @Test
        @DisplayName("should fetch favicon when website URL provided")
        void shouldFetchFaviconWhenWebsiteUrlProvided() {
            CreateServiceRequest request = CreateServiceRequest.builder()
                    .name("Spotify")
                    .category("Music")
                    .websiteUrl("https://spotify.com")
                    .build();

            FaviconService.FaviconData faviconData = new FaviconService.FaviconData(
                    new byte[]{1, 2, 3}, "image/png"
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.existsByUserIdAndName(1L, "Spotify")).thenReturn(false);
            when(faviconService.fetchFavicon("https://spotify.com")).thenReturn(Optional.of(faviconData));
            when(serviceRepository.save(any(Service.class))).thenReturn(testService);

            serviceService.createService(1L, request);

            ArgumentCaptor<Service> captor = ArgumentCaptor.forClass(Service.class);
            verify(serviceRepository).save(captor.capture());
            assertArrayEquals(new byte[]{1, 2, 3}, captor.getValue().getFavicon());
            assertEquals("image/png", captor.getValue().getFaviconContentType());
        }

        @Test
        @DisplayName("should throw exception when name already exists")
        void shouldThrowExceptionWhenNameAlreadyExists() {
            CreateServiceRequest request = CreateServiceRequest.builder()
                    .name("Netflix")
                    .category("Entertainment")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(serviceRepository.existsByUserIdAndName(1L, "Netflix")).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () ->
                    serviceService.createService(1L, request)
            );
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            CreateServiceRequest request = CreateServiceRequest.builder()
                    .name("Spotify")
                    .category("Music")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    serviceService.createService(1L, request)
            );
        }
    }

    @Nested
    @DisplayName("updateService")
    class UpdateService {

        @Test
        @DisplayName("should update service name")
        void shouldUpdateServiceName() {
            UpdateServiceRequest request = UpdateServiceRequest.builder()
                    .name("Netflix Premium")
                    .build();

            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(serviceRepository.existsByUserIdAndName(1L, "Netflix Premium")).thenReturn(false);
            when(serviceRepository.save(any(Service.class))).thenReturn(testService);
            when(serviceRepository.countSubscriptionsByServiceId(1L)).thenReturn(0L);

            ServiceResponse result = serviceService.updateService(1L, 1L, request);

            assertNotNull(result);
            ArgumentCaptor<Service> captor = ArgumentCaptor.forClass(Service.class);
            verify(serviceRepository).save(captor.capture());
            assertEquals("Netflix Premium", captor.getValue().getName());
        }

        @Test
        @DisplayName("should throw exception when new name already exists")
        void shouldThrowExceptionWhenNewNameAlreadyExists() {
            UpdateServiceRequest request = UpdateServiceRequest.builder()
                    .name("Spotify")
                    .build();

            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(serviceRepository.existsByUserIdAndName(1L, "Spotify")).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () ->
                    serviceService.updateService(1L, 1L, request)
            );
        }

        @Test
        @DisplayName("should re-fetch favicon when website URL changes")
        void shouldRefetchFaviconWhenWebsiteUrlChanges() {
            UpdateServiceRequest request = UpdateServiceRequest.builder()
                    .websiteUrl("https://new-netflix.com")
                    .build();

            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(faviconService.fetchFavicon("https://new-netflix.com")).thenReturn(Optional.empty());
            when(serviceRepository.save(any(Service.class))).thenReturn(testService);
            when(serviceRepository.countSubscriptionsByServiceId(1L)).thenReturn(0L);

            serviceService.updateService(1L, 1L, request);

            verify(faviconService).fetchFavicon("https://new-netflix.com");
        }

        @Test
        @DisplayName("should clear favicon when website URL cleared")
        void shouldClearFaviconWhenWebsiteUrlCleared() {
            testService.setFavicon(new byte[]{1, 2, 3});
            testService.setFaviconContentType("image/png");

            UpdateServiceRequest request = UpdateServiceRequest.builder()
                    .websiteUrl("")
                    .build();

            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(serviceRepository.save(any(Service.class))).thenReturn(testService);
            when(serviceRepository.countSubscriptionsByServiceId(1L)).thenReturn(0L);

            serviceService.updateService(1L, 1L, request);

            ArgumentCaptor<Service> captor = ArgumentCaptor.forClass(Service.class);
            verify(serviceRepository).save(captor.capture());
            assertNull(captor.getValue().getFavicon());
            assertNull(captor.getValue().getFaviconContentType());
        }
    }

    @Nested
    @DisplayName("deleteService")
    class DeleteService {

        @Test
        @DisplayName("should delete service when no subscriptions exist")
        void shouldDeleteServiceWhenNoSubscriptionsExist() {
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(serviceRepository.countSubscriptionsByServiceId(1L)).thenReturn(0L);

            serviceService.deleteService(1L, 1L);

            verify(serviceRepository).delete(testService);
        }

        @Test
        @DisplayName("should throw exception when service has subscriptions")
        void shouldThrowExceptionWhenServiceHasSubscriptions() {
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testService));
            when(serviceRepository.countSubscriptionsByServiceId(1L)).thenReturn(3L);

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    serviceService.deleteService(1L, 1L)
            );

            assertTrue(exception.getMessage().contains("3 subscription(s)"));
        }

        @Test
        @DisplayName("should throw exception when service not found")
        void shouldThrowExceptionWhenServiceNotFound() {
            when(serviceRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    serviceService.deleteService(1L, 1L)
            );
        }
    }

    @Nested
    @DisplayName("getCategories")
    class GetCategories {

        @Test
        @DisplayName("should return distinct categories for user")
        void shouldReturnDistinctCategoriesForUser() {
            List<String> categories = List.of("Entertainment", "Music", "Productivity");
            when(serviceRepository.findDistinctCategoriesByUserId(1L)).thenReturn(categories);

            List<String> result = serviceService.getCategories(1L);

            assertNotNull(result);
            assertEquals(3, result.size());
            assertTrue(result.contains("Entertainment"));
        }
    }
}
