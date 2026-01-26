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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final FaviconService faviconService;

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

    public List<ServiceResponse> getAllServicesForUser(Long userId) {
        return serviceRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(service -> {
                    long subscriptionCount = serviceRepository.countSubscriptionsByServiceId(service.getId());
                    return ServiceResponse.fromEntity(service, subscriptionCount);
                })
                .collect(Collectors.toList());
    }

    public ServiceResponse getService(Long userId, Long serviceId) {
        Service service = serviceRepository.findByIdAndUserId(serviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", serviceId));

        long subscriptionCount = serviceRepository.countSubscriptionsByServiceId(serviceId);
        return ServiceResponse.fromEntity(service, subscriptionCount);
    }

    @Transactional
    public ServiceResponse createService(Long userId, CreateServiceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (serviceRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new DuplicateResourceException("Service with this name already exists");
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

    @Transactional
    public ServiceResponse updateService(Long userId, Long serviceId, UpdateServiceRequest request) {
        Service service = serviceRepository.findByIdAndUserId(serviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", serviceId));

        if (request.getName() != null && !request.getName().equals(service.getName())) {
            if (serviceRepository.existsByUserIdAndName(userId, request.getName())) {
                throw new DuplicateResourceException("Service with this name already exists");
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

    @Transactional
    public void deleteService(Long userId, Long serviceId) {
        Service service = serviceRepository.findByIdAndUserId(serviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", serviceId));

        long subscriptionCount = serviceRepository.countSubscriptionsByServiceId(serviceId);
        if (subscriptionCount > 0) {
            throw new BadRequestException(
                    String.format("Cannot delete service. It is used in %d subscription(s).", subscriptionCount)
            );
        }

        serviceRepository.delete(service);
    }

    public List<String> getCategories(Long userId) {
        return serviceRepository.findDistinctCategoriesByUserId(userId);
    }
}
