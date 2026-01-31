package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.request.CreateServiceRequest;
import com.subscriptiontracker.dto.request.UpdateServiceRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.ServiceResponse;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<ServiceResponse>> getAllServices(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        PaginatedResponse<ServiceResponse> response = serviceService.getAllServices(userId, search, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ServiceResponse>> getAllServicesForDropdown(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        List<ServiceResponse> services = serviceService.getAllServicesForUser(userId);
        return ResponseEntity.ok(services);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getService(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        ServiceResponse response = serviceService.getService(userId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ServiceResponse> createService(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateServiceRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        ServiceResponse response = serviceService.createService(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceResponse> updateService(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        ServiceResponse response = serviceService.updateService(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        serviceService.deleteService(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        List<String> categories = serviceService.getCategories(userId);
        return ResponseEntity.ok(categories);
    }
}
