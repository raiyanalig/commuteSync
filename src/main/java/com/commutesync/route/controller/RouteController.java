package com.commutesync.route.controller;

import com.commutesync.common.api.PageResponse;
import com.commutesync.route.domain.RouteStatus;
import com.commutesync.route.dto.CreateRouteRequest;
import com.commutesync.route.dto.RouteResponse;
import com.commutesync.route.dto.RouteSummaryResponse;
import com.commutesync.route.dto.UpdateRouteRequest;
import com.commutesync.route.service.RouteService;
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
@RequestMapping("/api/routes")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Routes", description = "Route management (ADMIN only)")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a route, optionally with ordered stops")
    public RouteResponse create(@Valid @RequestBody CreateRouteRequest request) {
        return routeService.create(request);
    }

    @GetMapping
    @Operation(summary = "List routes with optional status/search filters")
    public PageResponse<RouteSummaryResponse> getAll(
            @RequestParam(required = false) RouteStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "routeCode") Pageable pageable) {
        return routeService.getAll(status, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a route with its ordered stops")
    public RouteResponse getById(@PathVariable Long id) {
        return routeService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a route")
    public RouteResponse update(@PathVariable Long id,
                                @Valid @RequestBody UpdateRouteRequest request) {
        return routeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a route and its stops")
    public void delete(@PathVariable Long id) {
        routeService.delete(id);
    }
}
