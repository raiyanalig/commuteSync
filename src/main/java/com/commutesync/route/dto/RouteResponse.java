package com.commutesync.route.dto;

import com.commutesync.route.domain.Route;
import com.commutesync.route.domain.RouteStatus;
import com.commutesync.route.domain.Stop;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record RouteResponse(
        Long id,
        String routeCode,
        String name,
        String source,
        String destination,
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        RouteStatus status,
        List<StopResponse> stops,
        Instant createdAt,
        Instant updatedAt
) {

    public static RouteResponse from(Route route) {
        List<StopResponse> orderedStops = route.getStops().stream()
                .sorted(Comparator.comparingInt(Stop::getStopOrder))
                .map(StopResponse::from)
                .toList();
        return new RouteResponse(
                route.getId(),
                route.getRouteCode(),
                route.getName(),
                route.getSource(),
                route.getDestination(),
                route.getDistanceKm(),
                route.getEstimatedDurationMinutes(),
                route.getStatus(),
                orderedStops,
                route.getCreatedAt(),
                route.getUpdatedAt()
        );
    }
}
