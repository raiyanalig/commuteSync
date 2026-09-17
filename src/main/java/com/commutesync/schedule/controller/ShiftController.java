package com.commutesync.schedule.controller;

import com.commutesync.common.api.PageResponse;
import com.commutesync.schedule.domain.ShiftStatus;
import com.commutesync.schedule.dto.CreateShiftRequest;
import com.commutesync.schedule.dto.EmployeeShiftResponse;
import com.commutesync.schedule.dto.ShiftResponse;
import com.commutesync.schedule.dto.UpdateShiftRequest;
import com.commutesync.schedule.service.ShiftAssignmentService;
import com.commutesync.schedule.service.ShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shifts")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Shifts", description = "Shift definitions and employee shift assignment (ADMIN only)")
public class ShiftController {

    private final ShiftService shiftService;
    private final ShiftAssignmentService shiftAssignmentService;

    public ShiftController(ShiftService shiftService,
                           ShiftAssignmentService shiftAssignmentService) {
        this.shiftService = shiftService;
        this.shiftAssignmentService = shiftAssignmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a shift")
    public ShiftResponse create(@Valid @RequestBody CreateShiftRequest request) {
        return shiftService.create(request);
    }

    @GetMapping
    @Operation(summary = "List shifts with optional status/search filters")
    public PageResponse<ShiftResponse> getAll(
            @RequestParam(required = false) ShiftStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "shiftCode") Pageable pageable) {
        return shiftService.getAll(status, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a shift by id")
    public ShiftResponse getById(@PathVariable Long id) {
        return shiftService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a shift")
    public ShiftResponse update(@PathVariable Long id,
                                @Valid @RequestBody UpdateShiftRequest request) {
        return shiftService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a shift")
    public void delete(@PathVariable Long id) {
        shiftService.delete(id);
    }

    @PostMapping("/{shiftId}/employees/{employeeId}")
    @Operation(summary = "Assign an employee to a shift")
    public EmployeeShiftResponse assignEmployee(@PathVariable Long shiftId,
                                                @PathVariable Long employeeId) {
        return shiftAssignmentService.assignEmployee(shiftId, employeeId);
    }

    @DeleteMapping("/{shiftId}/employees/{employeeId}")
    @Operation(summary = "Remove an employee from a shift")
    public EmployeeShiftResponse unassignEmployee(@PathVariable Long shiftId,
                                                  @PathVariable Long employeeId) {
        return shiftAssignmentService.unassignEmployee(shiftId, employeeId);
    }

    @GetMapping("/{shiftId}/employees")
    @Operation(summary = "List employees assigned to a shift")
    public List<EmployeeShiftResponse> getEmployees(@PathVariable Long shiftId) {
        return shiftAssignmentService.getEmployees(shiftId);
    }
}
