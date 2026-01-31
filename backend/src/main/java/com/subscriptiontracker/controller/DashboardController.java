package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.response.DashboardSummaryResponse;
import com.subscriptiontracker.dto.response.ProjectionResponse;
import com.subscriptiontracker.service.CurrentUserService;
import com.subscriptiontracker.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserService currentUserService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
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

    @GetMapping("/projection")
    public ResponseEntity<ProjectionResponse> getProjection(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = currentUserService.getCurrentUserId(userDetails);
        ProjectionResponse response = dashboardService.getProjection(userId);
        return ResponseEntity.ok(response);
    }
}
