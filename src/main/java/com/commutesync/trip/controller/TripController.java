package com.commutesync.trip.controller;

import com.commutesync.common.api.PageResponse;
import com.commutesync.trip.domain.TripStatus;
import com.commutesync.trip.dto.AssignTripRequest;
import com.commutesync.trip.dto.CancelTripRequest;
import com.commutesync.trip.dto.CreateTripRequest;
import com.commutesync.trip.dto.TripResponse;
import com.commutesync.trip.dto.UpdateTripStatusRequest;
import com.commutesync.trip.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Trips", description = "Trip lifecycle management (ADMIN only)")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a trip, optionally generated from a schedule")
    public TripResponse create(@Valid @RequestBody CreateTripRequest request) {
        return tripService.create(request);
    }

    @GetMapping
    @Operation(summary = "List/filter trips by date, status, route, driver or vehicle")
    public PageResponse<TripResponse> getAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate serviceDate,
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long vehicleId,
            @PageableDefault(size = 10, sort = "serviceDate") Pageable pageable) {
        return tripService.getAll(serviceDate, status, routeId, driverId, vehicleId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a trip by id")
    public TripResponse getById(@PathVariable Long id) {
        return tripService.getById(id);
    }

    @PutMapping("/{id}/assignment")
    @Operation(summary = "Assign a driver and vehicle to a trip")
    public TripResponse assign(@PathVariable Long id,
                               @Valid @RequestBody AssignTripRequest request) {
        return tripService.assign(id, request);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start an assigned trip")
    public TripResponse start(@PathVariable Long id) {
        return tripService.start(id);
    }

    @PostMapping("/{id}/progress")
    @Operation(summary = "Mark a started trip as in progress")
    public TripResponse markInProgress(@PathVariable Long id) {
        return tripService.markInProgress(id);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete an in-progress trip")
    public TripResponse complete(@PathVariable Long id) {
        return tripService.complete(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a trip")
    public TripResponse cancel(@PathVariable Long id,
                               @Valid @RequestBody(required = false) CancelTripRequest request) {
        return tripService.cancel(id, request == null ? null : request.reason());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transition a trip to a new status")
    public TripResponse changeStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateTripStatusRequest request) {
        return tripService.changeStatus(id, request.status(), request.reason());
    }
}
