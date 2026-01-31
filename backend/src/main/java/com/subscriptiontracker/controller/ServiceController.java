package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.request.CreateServiceRequest;
import com.subscriptiontracker.dto.request.UpdateServiceRequest;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.dto.response.ServiceResponse;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.ServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for service catalog management.
 *
 * <p>Handles CRUD operations for services (Netflix, Spotify, etc.) that can be
 * associated with subscriptions.</p>
 *
 * @author Generated
 * @since 1.0
 */
@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Service catalog management")
public class ServiceController {

    private final ServiceService serviceService;
    private final CurrentUserService currentUserService;

    /**
     * Retrieves a paginated list of services.
     *
     * @param userDetails the authenticated user details
     * @param search optional search term
     * @param page page number (1-based)
     * @param limit number of items per page
     * @return paginated list of services
     */
    @Operation(
            summary = "List services",
            description = "Retrieves a paginated list of services available to the user, "
                    + "including global services and user-created custom services."
    )
    @ApiResponse(responseCode = "200", description = "Services retrieved successfully")
    @GetMapping
    public ResponseEntity<PaginatedResponse<ServiceResponse>> getAllServices(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Search term for service name") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "20") int limit
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        PaginatedResponse<ServiceResponse> response = serviceService.getAllServices(userId, search, page, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all services for dropdown selection (non-paginated).
     *
     * @param userDetails the authenticated user details
     * @return list of all services
     */
    @Operation(
            summary = "List all services for dropdown",
            description = "Retrieves all services in a non-paginated format, suitable for dropdown menus."
    )
    @ApiResponse(responseCode = "200", description = "Services retrieved successfully")
    @GetMapping("/all")
    public ResponseEntity<List<ServiceResponse>> getAllServicesForDropdown(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        List<ServiceResponse> services = serviceService.getAllServicesForUser(userId);
        return ResponseEntity.ok(services);
    }

    /**
     * Retrieves a specific service by ID.
     *
     * @param userDetails the authenticated user details
     * @param id the service ID
     * @return the service details
     */
    @Operation(
            summary = "Get service details",
            description = "Retrieves detailed information about a specific service."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getService(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Service ID") @PathVariable Long id
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        ServiceResponse response = serviceService.getService(userId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new custom service.
     *
     * @param userDetails the authenticated user details
     * @param request the service creation request
     * @return the created service
     */
    @Operation(
            summary = "Create service",
            description = "Creates a new custom service for the user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Service created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping
    public ResponseEntity<ServiceResponse> createService(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateServiceRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        ServiceResponse response = serviceService.createService(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing custom service.
     *
     * @param userDetails the authenticated user details
     * @param id the service ID
     * @param request the update request
     * @return the updated service
     */
    @Operation(
            summary = "Update service",
            description = "Updates an existing custom service. Only user-created services can be modified."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or cannot modify global service"),
            @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ServiceResponse> updateService(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Service ID") @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        ServiceResponse response = serviceService.updateService(userId, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a custom service.
     *
     * @param userDetails the authenticated user details
     * @param id the service ID
     * @return empty response on successful deletion
     */
    @Operation(
            summary = "Delete service",
            description = "Deletes a custom service. Only user-created services can be deleted."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Service deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot delete global service or service in use"),
            @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Service ID") @PathVariable Long id
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        serviceService.deleteService(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all unique service categories.
     *
     * @param userDetails the authenticated user details
     * @return list of category names
     */
    @Operation(
            summary = "Get service categories",
            description = "Retrieves a list of all unique categories from available services."
    )
    @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        List<String> categories = serviceService.getCategories(userId);
        return ResponseEntity.ok(categories);
    }
}
