package com.commutesync.admin.controller;

import com.commutesync.admin.dto.DashboardResponse;
import com.commutesync.admin.dto.FleetSummaryResponse;
import com.commutesync.admin.dto.TripStatusSummaryResponse;
import com.commutesync.admin.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Operational metrics and dashboard (ADMIN only)")
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    public AdminController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Headline operational metrics")
    public DashboardResponse getDashboard() {
        return adminDashboardService.getDashboard();
    }

    @GetMapping("/trips/summary")
    @Operation(summary = "Trip counts grouped by status, optionally filtered by service date")
    public TripStatusSummaryResponse getTripSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate serviceDate) {
        return adminDashboardService.getTripSummary(serviceDate);
    }

    @GetMapping("/fleet/summary")
    @Operation(summary = "Vehicle and driver counts grouped by status")
    public FleetSummaryResponse getFleetSummary() {
        return adminDashboardService.getFleetSummary();
    }
}
