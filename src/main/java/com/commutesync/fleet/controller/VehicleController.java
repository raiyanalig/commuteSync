package com.commutesync.fleet.controller;

import com.commutesync.common.api.PageResponse;
import com.commutesync.fleet.domain.VehicleStatus;
import com.commutesync.fleet.dto.CreateVehicleRequest;
import com.commutesync.fleet.dto.UpdateVehicleRequest;
import com.commutesync.fleet.dto.UpdateVehicleStatusRequest;
import com.commutesync.fleet.dto.VehicleResponse;
import com.commutesync.fleet.service.DriverAssignmentService;
import com.commutesync.fleet.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/vehicles")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Vehicles", description = "Vehicle management and driver assignment (ADMIN only)")
public class VehicleController {

    private final VehicleService vehicleService;
    private final DriverAssignmentService driverAssignmentService;

    public VehicleController(VehicleService vehicleService,
                             DriverAssignmentService driverAssignmentService) {
        this.vehicleService = vehicleService;
        this.driverAssignmentService = driverAssignmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a vehicle")
    public VehicleResponse create(@Valid @RequestBody CreateVehicleRequest request) {
        return vehicleService.create(request);
    }

    @GetMapping
    @Operation(summary = "List vehicles with optional status/search filters")
    public PageResponse<VehicleResponse> getAll(
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "registrationNumber") Pageable pageable) {
        return vehicleService.getAll(status, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a vehicle by id")
    public VehicleResponse getById(@PathVariable Long id) {
        return vehicleService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a vehicle")
    public VehicleResponse update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateVehicleRequest request) {
        return vehicleService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update a vehicle's status")
    public VehicleResponse updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody UpdateVehicleStatusRequest request) {
        return vehicleService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a vehicle")
    public void delete(@PathVariable Long id) {
        vehicleService.delete(id);
    }

    @PutMapping("/{vehicleId}/driver/{driverId}")
    @Operation(summary = "Assign a driver to a vehicle")
    public VehicleResponse assignDriver(@PathVariable Long vehicleId, @PathVariable Long driverId) {
        return driverAssignmentService.assignDriverToVehicle(vehicleId, driverId);
    }

    @DeleteMapping("/{vehicleId}/driver")
    @Operation(summary = "Unassign the driver from a vehicle")
    public VehicleResponse unassignDriver(@PathVariable Long vehicleId) {
        return driverAssignmentService.unassignDriverFromVehicle(vehicleId);
    }
}
