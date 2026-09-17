package com.commutesync.employee.controller;

import com.commutesync.common.api.PageResponse;
import com.commutesync.employee.domain.EmployeeStatus;
import com.commutesync.employee.dto.CreateEmployeeRequest;
import com.commutesync.employee.dto.EmployeeResponse;
import com.commutesync.employee.dto.UpdateEmployeeRequest;
import com.commutesync.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/employees")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Employees", description = "Employee management (ADMIN only)")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an employee")
    public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest request) {
        return employeeService.create(request);
    }

    @GetMapping
    @Operation(summary = "List employees with optional status/search filters and pagination")
    public PageResponse<EmployeeResponse> getAll(
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "employeeCode") Pageable pageable) {
        return employeeService.getAll(status, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an employee by id")
    public EmployeeResponse getById(@PathVariable Long id) {
        return employeeService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an employee")
    public EmployeeResponse update(@PathVariable Long id,
                                   @Valid @RequestBody UpdateEmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an employee")
    public void delete(@PathVariable Long id) {
        employeeService.delete(id);
    }
}
