package com.commutesync.fleet.controller;

import com.commutesync.common.api.PageResponse;
import com.commutesync.fleet.domain.DriverStatus;
import com.commutesync.fleet.dto.CreateDriverRequest;
import com.commutesync.fleet.dto.DriverResponse;
import com.commutesync.fleet.dto.UpdateDriverAvailabilityRequest;
import com.commutesync.fleet.dto.UpdateDriverRequest;
import com.commutesync.fleet.service.DriverService;
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
@RequestMapping("/api/drivers")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Drivers", description = "Driver management (ADMIN only)")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a driver")
    public DriverResponse create(@Valid @RequestBody CreateDriverRequest request) {
        return driverService.create(request);
    }

    @GetMapping
    @Operation(summary = "List drivers with optional status/availability/search filters")
    public PageResponse<DriverResponse> getAll(
            @RequestParam(required = false) DriverStatus status,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "fullName") Pageable pageable) {
        return driverService.getAll(status, available, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a driver by id")
    public DriverResponse getById(@PathVariable Long id) {
        return driverService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a driver")
    public DriverResponse update(@PathVariable Long id,
                                 @Valid @RequestBody UpdateDriverRequest request) {
        return driverService.update(id, request);
    }

    @PatchMapping("/{id}/availability")
    @Operation(summary = "Update a driver's operational availability")
    public DriverResponse updateAvailability(@PathVariable Long id,
                                             @Valid @RequestBody UpdateDriverAvailabilityRequest request) {
        return driverService.updateAvailability(id, request.available());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a driver")
    public void delete(@PathVariable Long id) {
        driverService.delete(id);
    }
}
