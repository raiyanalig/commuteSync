package com.commutesync.schedule.controller;

import com.commutesync.common.api.PageResponse;
import com.commutesync.schedule.domain.ScheduleStatus;
import com.commutesync.schedule.dto.CreateScheduleRequest;
import com.commutesync.schedule.dto.ScheduleResponse;
import com.commutesync.schedule.dto.UpdateScheduleRequest;
import com.commutesync.schedule.dto.UpdateScheduleStatusRequest;
import com.commutesync.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/schedules")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Schedules", description = "Dated transportation schedules (ADMIN only)")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a schedule")
    public ScheduleResponse create(@Valid @RequestBody CreateScheduleRequest request) {
        return scheduleService.create(request);
    }

    @GetMapping
    @Operation(summary = "List schedules by service date, status and route")
    public PageResponse<ScheduleResponse> getAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate serviceDate,
            @RequestParam(required = false) ScheduleStatus status,
            @RequestParam(required = false) Long routeId,
            @PageableDefault(size = 10, sort = "serviceDate") Pageable pageable) {
        return scheduleService.getAll(serviceDate, status, routeId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a schedule by id")
    public ScheduleResponse getById(@PathVariable Long id) {
        return scheduleService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a schedule")
    public ScheduleResponse update(@PathVariable Long id,
                                   @Valid @RequestBody UpdateScheduleRequest request) {
        return scheduleService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a schedule")
    public ScheduleResponse updateStatus(@PathVariable Long id,
                                         @Valid @RequestBody UpdateScheduleStatusRequest request) {
        return scheduleService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a schedule")
    public void delete(@PathVariable Long id) {
        scheduleService.delete(id);
    }
}
