package com.commutesync.tracking.controller;

import com.commutesync.common.api.PageResponse;
import com.commutesync.tracking.dto.TrackingUpdateRequest;
import com.commutesync.tracking.dto.TripLocationResponse;
import com.commutesync.tracking.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/tracking")
@Tag(name = "Tracking", description = "Trip location tracking")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','DRIVER')")
    @Operation(summary = "Record a driver location update for a trip")
    public TripLocationResponse recordLocation(@PathVariable Long tripId,
                                               @Valid @RequestBody TrackingUpdateRequest request) {
        return trackingService.recordLocation(tripId, request);
    }

    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('ADMIN','DRIVER','EMPLOYEE')")
    @Operation(summary = "Get the most recent location of a trip")
    public TripLocationResponse getLatestLocation(@PathVariable Long tripId) {
        return trackingService.getLatestLocation(tripId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DRIVER','EMPLOYEE')")
    @Operation(summary = "Get the location history of a trip (newest first)")
    public PageResponse<TripLocationResponse> getHistory(
            @PathVariable Long tripId,
            @PageableDefault(size = 20, sort = "recordedAt") Pageable pageable) {
        return trackingService.getHistory(tripId, pageable);
    }
}
