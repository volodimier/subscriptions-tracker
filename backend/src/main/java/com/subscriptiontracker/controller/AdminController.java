package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.response.JobRunResponse;
import com.subscriptiontracker.dto.response.PaginatedResponse;
import com.subscriptiontracker.service.JobRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
}
