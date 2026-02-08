package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.request.CreateCategoryRequest;
import com.subscriptiontracker.dto.request.UpdateCategoryRequest;
import com.subscriptiontracker.dto.response.CategoryResponse;
import com.subscriptiontracker.dto.response.JobRunResponse;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.service.CategoryService;
import com.subscriptiontracker.service.JobRunService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for administrative operations.
 *
 * <p>All endpoints in this controller require the ADMIN role.
 * Provides access to system-level information and operations
 * such as job run history.</p>
 *
 * @author Generated
 * @since 1.0
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administrative operations (requires ADMIN role)")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final JobRunService jobRunService;
    private final CategoryService categoryService;

    /**
     * Retrieves paginated job run history.
     *
     * <p>Returns all job executions ordered by finish time descending,
     * including both successful and failed runs.</p>
     *
     * @param page  page number (1-based, default 1)
     * @param limit items per page (default 20)
     * @return paginated list of job run records
     */
    @Operation(
            summary = "List job run history",
            description = "Retrieves a paginated list of all job executions. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job runs retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized - requires ADMIN role")
    })
    @GetMapping("/job-runs")
    public ResponseEntity<PaginatedResponse<JobRunResponse>> getJobRuns(
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "20") int limit
    ) {
        PaginatedResponse<JobRunResponse> response = jobRunService.getAllJobRuns(page, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a specific job run by ID.
     *
     * @param id the job run ID
     * @return the job run details
     */
    @Operation(
            summary = "Get job run by ID",
            description = "Retrieves details of a specific job execution. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job run retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Job run not found")
    })
    @GetMapping("/job-runs/{id}")
    public ResponseEntity<JobRunResponse> getJobRunById(
            @Parameter(description = "Job run ID") @PathVariable Long id
    ) {
        JobRunResponse response = jobRunService.getJobRunById(id);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Category Management Endpoints
    // =========================================================================

    /**
     * Retrieves all system categories.
     *
     * @return list of system categories sorted alphabetically
     */
    @Operation(
            summary = "List system categories",
            description = "Retrieves all system categories. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized - requires ADMIN role")
    })
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getSystemCategories() {
        List<CategoryResponse> categories = categoryService.getSystemCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Creates a new system category.
     *
     * @param request the category creation request
     * @return the created category
     */
    @Operation(
            summary = "Create system category",
            description = "Creates a new system category. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized - requires ADMIN role"),
            @ApiResponse(responseCode = "409", description = "Category with this name already exists")
    })
    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createSystemCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        CategoryResponse response = categoryService.createSystemCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing system category.
     *
     * @param id      the category ID
     * @param request the category update request
     * @return the updated category
     */
    @Operation(
            summary = "Update system category",
            description = "Updates an existing system category. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "409", description = "Category with this name already exists")
    })
    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> updateSystemCategory(
            @Parameter(description = "Category ID") @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        CategoryResponse response = categoryService.updateSystemCategory(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a system category.
     *
     * @param id the category ID
     * @return empty response on successful deletion
     */
    @Operation(
            summary = "Delete system category",
            description = "Deletes a system category. Cannot delete if in use by services. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Category is in use and cannot be deleted"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteSystemCategory(
            @Parameter(description = "Category ID") @PathVariable Long id
    ) {
        categoryService.deleteSystemCategory(id);
        return ResponseEntity.noContent().build();
    }
}
