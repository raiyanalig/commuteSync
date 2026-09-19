package com.commutesync.allocation.controller;

import com.commutesync.allocation.dto.AllocationRequest;
import com.commutesync.allocation.dto.AllocationResponse;
import com.commutesync.allocation.dto.VehicleAssignmentResponse;
import com.commutesync.allocation.service.AllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/allocations")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Allocations", description = "Vehicle allocation engine (ADMIN only)")
public class AllocationController {

    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @PostMapping
    @Operation(summary = "Run capacity-based greedy allocation for a schedule")
    public AllocationResponse allocate(@Valid @RequestBody AllocationRequest request) {
        return allocationService.allocate(request);
    }

    @GetMapping("/schedule/{scheduleId}")
    @Operation(summary = "Get the current vehicle assignments for a schedule")
    public List<VehicleAssignmentResponse> getAssignments(@PathVariable Long scheduleId) {
        return allocationService.getAssignments(scheduleId);
    }
}
