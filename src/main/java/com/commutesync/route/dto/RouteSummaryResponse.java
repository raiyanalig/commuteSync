package com.commutesync.route.dto;

import com.commutesync.route.domain.Route;
import com.commutesync.route.domain.RouteStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record RouteSummaryResponse(
        Long id,
        String routeCode,
        String name,
        String source,
        String destination,
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        RouteStatus status,
        Instant createdAt
) {

    public static RouteSummaryResponse from(Route route) {
        return new RouteSummaryResponse(
                route.getId(),
                route.getRouteCode(),
                route.getName(),
                route.getSource(),
                route.getDestination(),
                route.getDistanceKm(),
                route.getEstimatedDurationMinutes(),
                route.getStatus(),
                route.getCreatedAt()
        );
    }
}
