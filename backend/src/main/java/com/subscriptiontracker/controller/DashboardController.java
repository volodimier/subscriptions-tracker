package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.response.DashboardSummaryResponse;
import com.subscriptiontracker.dto.response.ProjectionResponse;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller for dashboard and analytics endpoints.
 *
 * <p>Provides spending summaries, projections, and analytics for the user's subscriptions.</p>
 *
 * @author Generated
 * @since 1.0
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Spending analytics and projections")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserService currentUserService;

    /**
     * Retrieves spending summary for a given period.
     *
     * @param userDetails the authenticated user details
     * @param startDate the start date of the period (defaults to first day of current month)
     * @param endDate the end date of the period (defaults to last day of current month)
     * @return the spending summary including totals, category breakdown, and top services
     */
    @Operation(
            summary = "Get spending summary",
            description = "Retrieves a comprehensive spending summary for the specified period, "
                    + "including total spent, category breakdown, and top services. "
                    + "Defaults to current month if dates are not specified."
    )
    @ApiResponse(responseCode = "200", description = "Summary retrieved successfully")
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Start date (ISO format: YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (ISO format: YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);

        // Default to current month if not specified
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        }

        DashboardSummaryResponse response = dashboardService.getSummary(userId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves spending projections for upcoming months.
     *
     * @param userDetails the authenticated user details
     * @return the spending projection
     */
    @Operation(
            summary = "Get spending projection",
            description = "Retrieves projected spending for upcoming months based on active subscriptions."
    )
    @ApiResponse(responseCode = "200", description = "Projection retrieved successfully")
    @GetMapping("/projection")
    public ResponseEntity<ProjectionResponse> getProjection(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        ProjectionResponse response = dashboardService.getProjection(userId);
        return ResponseEntity.ok(response);
    }
}
