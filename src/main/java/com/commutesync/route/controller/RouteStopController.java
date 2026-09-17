package com.commutesync.route.controller;

import com.commutesync.route.dto.CreateStopRequest;
import com.commutesync.route.dto.ReorderStopsRequest;
import com.commutesync.route.dto.RouteResponse;
import com.commutesync.route.dto.StopResponse;
import com.commutesync.route.dto.UpdateStopRequest;
import com.commutesync.route.service.RouteStopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes/{routeId}/stops")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Route Stops", description = "Ordered stop management for a route (ADMIN only)")
public class RouteStopController {

    private final RouteStopService routeStopService;

    public RouteStopController(RouteStopService routeStopService) {
        this.routeStopService = routeStopService;
    }

    @GetMapping
    @Operation(summary = "List a route's stops in order")
    public List<StopResponse> getStops(@PathVariable Long routeId) {
        return routeStopService.getStops(routeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Append a stop to the end of the route")
    public RouteResponse addStop(@PathVariable Long routeId,
                                 @Valid @RequestBody CreateStopRequest request) {
        return routeStopService.addStop(routeId, request);
    }

    @PutMapping("/{stopId}")
    @Operation(summary = "Update a stop's name or address")
    public StopResponse updateStop(@PathVariable Long routeId,
                                   @PathVariable Long stopId,
                                   @Valid @RequestBody UpdateStopRequest request) {
        return routeStopService.updateStop(routeId, stopId, request);
    }

    @DeleteMapping("/{stopId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a stop and renumber the remaining stops")
    public void removeStop(@PathVariable Long routeId, @PathVariable Long stopId) {
        routeStopService.removeStop(routeId, stopId);
    }

    @PutMapping("/order")
    @Operation(summary = "Reorder all stops by providing the full list of stop ids in the new order")
    public RouteResponse reorderStops(@PathVariable Long routeId,
                                      @Valid @RequestBody ReorderStopsRequest request) {
        return routeStopService.reorderStops(routeId, request.stopIds());
    }
}
