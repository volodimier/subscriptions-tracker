package com.subscriptiontracker.service;

import com.subscriptiontracker.constant.DomainConstants;
import com.subscriptiontracker.constant.ErrorMessages;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling service catalog management operations.
 *
 * <p>Manages the catalog of subscription services that users can subscribe to
 * (e.g., Netflix, Spotify, gym memberships). Each user has their own set of
 * services with unique names.</p>
 *
 * <p>Features include:</p>
 * <ul>
 *   <li>Creating services with automatic favicon fetching</li>
 *   <li>Updating service details with favicon refresh on URL change</li>
 *   <li>Preventing deletion of services with active subscriptions</li>
 *   <li>Category management for organizing services</li>
 * </ul>
 *
 * @author Generated
 * @since 1.0
 * @see Service
 * @see ServiceRepository
 * @see FaviconService
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final FaviconService faviconService;

    /**
     * Retrieves a paginated list of services for a user.
     *
     * @param userId the ID of the user whose services to retrieve
     * @param search optional search term for filtering by service name
     * @param page   page number (1-based)
     * @param limit  number of items per page
     * @return paginated response containing service data with subscription counts
     */
    public PaginatedResponse<ServiceResponse> getAllServices(Long userId, String search, int page, int limit) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by("name").ascending());

        Page<Service> servicePage;
        if (search != null && !search.trim().isEmpty()) {
            servicePage = serviceRepository.findByUserIdAndNameContaining(userId, search.trim(), pageRequest);
        } else {
            servicePage = serviceRepository.findByUserId(userId, pageRequest);
        }

        List<ServiceResponse> services = servicePage.getContent().stream()
                .map(service -> {
                    long subscriptionCount = serviceRepository.countSubscriptionsByServiceId(service.getId());
                    return ServiceResponse.fromEntity(service, subscriptionCount);
                })
                .collect(Collectors.toList());

        return PaginatedResponse.of(services, page, limit, servicePage.getTotalElements());
    }

    /**
     * Retrieves all services for a user without pagination.
     *
     * <p>Intended for dropdown menus where all services need to be displayed.</p>
     *
     * @param userId the ID of the user whose services to retrieve
     * @return list of all services sorted by name
     */
    public List<ServiceResponse> getAllServicesForUser(Long userId) {
        return serviceRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(service -> {
                    long subscriptionCount = serviceRepository.countSubscriptionsByServiceId(service.getId());
                    return ServiceResponse.fromEntity(service, subscriptionCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific service by ID.
     *
     * @param userId    the ID of the user who owns the service
     * @param serviceId the ID of the service to retrieve
     * @return the service response with subscription count
     * @throws ResourceNotFoundException if the service is not found
     */
    public ServiceResponse getService(Long userId, Long serviceId) {
        Service service = serviceRepository.findByIdAndUserId(serviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_SERVICE, DomainConstants.FIELD_ID, serviceId));

        long subscriptionCount = serviceRepository.countSubscriptionsByServiceId(serviceId);
        return ServiceResponse.fromEntity(service, subscriptionCount);
    }

    /**
     * Creates a new service for a user.
     *
     * <p>If a website URL is provided, automatically attempts to fetch the
     * favicon for visual identification in the UI.</p>
     *
     * @param userId  the ID of the user creating the service
     * @param request the service creation request
     * @return the created service response
     * @throws ResourceNotFoundException   if the user is not found
     * @throws DuplicateResourceException if a service with the same name already exists
     */
    @Transactional
    public ServiceResponse createService(Long userId, CreateServiceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_USER, DomainConstants.FIELD_ID, userId));

        if (serviceRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new DuplicateResourceException(ErrorMessages.SERVICE_NAME_ALREADY_EXISTS);
        }

        Service service = Service.builder()
                .user(user)
                .name(request.getName())
                .category(request.getCategory())
                .websiteUrl(request.getWebsiteUrl())
                .build();

        // Auto-fetch favicon if website URL is provided
        if (request.getWebsiteUrl() != null && !request.getWebsiteUrl().isBlank()) {
            var faviconOpt = faviconService.fetchFavicon(request.getWebsiteUrl());
            if (faviconOpt.isPresent()) {
                service.setFavicon(faviconOpt.get().data());
                service.setFaviconContentType(faviconOpt.get().contentType());
            }
        }

        Service savedService = serviceRepository.save(service);
        return ServiceResponse.fromEntity(savedService, 0);
    }

    /**
     * Updates an existing service.
     *
     * <p>Only non-null fields in the request are updated. If the website URL
     * changes, the favicon is automatically re-fetched.</p>
     *
     * @param userId    the ID of the user who owns the service
     * @param serviceId the ID of the service to update
     * @param request   the update request with fields to modify
     * @return the updated service response
     * @throws ResourceNotFoundException   if the service is not found
     * @throws DuplicateResourceException if the new name already exists for another service
     */
    @Transactional
    public ServiceResponse updateService(Long userId, Long serviceId, UpdateServiceRequest request) {
        Service service = serviceRepository.findByIdAndUserId(serviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_SERVICE, DomainConstants.FIELD_ID, serviceId));

        if (request.getName() != null && !request.getName().equals(service.getName())) {
            if (serviceRepository.existsByUserIdAndName(userId, request.getName())) {
                throw new DuplicateResourceException(ErrorMessages.SERVICE_NAME_ALREADY_EXISTS);
            }
            service.setName(request.getName());
        }

        if (request.getCategory() != null) {
            service.setCategory(request.getCategory());
        }

        if (request.getWebsiteUrl() != null) {
            String oldUrl = service.getWebsiteUrl();
            service.setWebsiteUrl(request.getWebsiteUrl());

            // Re-fetch favicon if website URL changed
            if (!request.getWebsiteUrl().equals(oldUrl)) {
                if (request.getWebsiteUrl().isBlank()) {
                    service.setFavicon(null);
                    service.setFaviconContentType(null);
                } else {
                    var faviconOpt = faviconService.fetchFavicon(request.getWebsiteUrl());
                    if (faviconOpt.isPresent()) {
                        service.setFavicon(faviconOpt.get().data());
                        service.setFaviconContentType(faviconOpt.get().contentType());
                    }
                }
            }
        }

        Service savedService = serviceRepository.save(service);
        long subscriptionCount = serviceRepository.countSubscriptionsByServiceId(serviceId);
        return ServiceResponse.fromEntity(savedService, subscriptionCount);
    }

    /**
     * Deletes a service.
     *
     * <p>A service cannot be deleted if it has any associated subscriptions.
     * Users must first delete or reassign all subscriptions using this service.</p>
     *
     * @param userId    the ID of the user who owns the service
     * @param serviceId the ID of the service to delete
     * @throws ResourceNotFoundException if the service is not found
     * @throws BadRequestException       if the service has associated subscriptions
     */
    @Transactional
    public void deleteService(Long userId, Long serviceId) {
        Service service = serviceRepository.findByIdAndUserId(serviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.RESOURCE_SERVICE, DomainConstants.FIELD_ID, serviceId));

        long subscriptionCount = serviceRepository.countSubscriptionsByServiceId(serviceId);
        if (subscriptionCount > 0) {
            throw new BadRequestException(
                    String.format(ErrorMessages.SERVICE_HAS_SUBSCRIPTIONS_TEMPLATE, subscriptionCount)
            );
        }

        serviceRepository.delete(service);
    }

    /**
     * Retrieves all unique categories from the user's services.
     *
     * @param userId the ID of the user
     * @return list of distinct category names sorted alphabetically
     */
    public List<String> getCategories(Long userId) {
        return serviceRepository.findDistinctCategoriesByUserId(userId);
    }
}
