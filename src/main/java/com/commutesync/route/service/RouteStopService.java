package com.commutesync.route.service;

import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.route.domain.Route;
import com.commutesync.route.domain.Stop;
import com.commutesync.route.dto.CreateStopRequest;
import com.commutesync.route.dto.RouteResponse;
import com.commutesync.route.dto.StopResponse;
import com.commutesync.route.dto.UpdateStopRequest;
import com.commutesync.route.repository.RouteRepository;
import com.commutesync.route.repository.StopRepository;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RouteStopService {

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;

    public RouteStopService(RouteRepository routeRepository, StopRepository stopRepository) {
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
    }

    @Transactional(readOnly = true)
    public List<StopResponse> getStops(Long routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new ResourceNotFoundException("Route", routeId);
        }
        return stopRepository.findByRouteIdOrderByStopOrderAsc(routeId).stream()
                .map(StopResponse::from)
                .toList();
    }

    @Transactional
    public RouteResponse addStop(Long routeId, CreateStopRequest request) {
        Route route = findRouteWithStops(routeId);

        int nextOrder = route.getStops().stream()
                .mapToInt(Stop::getStopOrder)
                .max()
                .orElse(0) + 1;

        Stop stop = new Stop();
        stop.setName(request.name().trim());
        stop.setAddress(trimToNull(request.address()));
        stop.setStopOrder(nextOrder);
        route.addStop(stop);

        return RouteResponse.from(routeRepository.save(route));
    }

    @Transactional
    public StopResponse updateStop(Long routeId, Long stopId, UpdateStopRequest request) {
        Route route = findRouteWithStops(routeId);
        Stop stop = findStop(route, stopId);
        stop.setName(request.name().trim());
        stop.setAddress(trimToNull(request.address()));
        return StopResponse.from(stop);
    }

    @Transactional
    public void removeStop(Long routeId, Long stopId) {
        Route route = findRouteWithStops(routeId);
        Stop stop = findStop(route, stopId);
        route.removeStop(stop);
        renumber(route.getStops());
    }

    @Transactional
    public RouteResponse reorderStops(Long routeId, List<Long> stopIds) {
        Route route = findRouteWithStops(routeId);
        List<Stop> stops = route.getStops();

        if (stopIds.size() != stops.size()) {
            throw new BusinessException(
                    "The order must contain exactly " + stops.size() + " stop ids");
        }

        Map<Long, Stop> stopsById = stops.stream()
                .collect(Collectors.toMap(Stop::getId, Function.identity()));
        if (!stopsById.keySet().equals(new HashSet<>(stopIds))) {
            throw new BusinessException("The order contains unknown or duplicate stop ids");
        }

        List<Stop> ordered = stopIds.stream().map(stopsById::get).toList();
        applyOrder(ordered);

        return RouteResponse.from(route);
    }

    private void renumber(List<Stop> stops) {
        List<Stop> ordered = stops.stream()
                .sorted(Comparator.comparingInt(Stop::getStopOrder))
                .toList();
        applyOrder(ordered);
    }

    /**
     * Two-phase update: stop_order has a unique constraint per route, so moving stops
     * (e.g. 1 -> 2 while another is still 2) would transiently collide. Assigning
     * temporary negative orders first, flushing, then assigning the final order avoids
     * the collision on both PostgreSQL and H2.
     */
    private void applyOrder(List<Stop> orderedStops) {
        for (int i = 0; i < orderedStops.size(); i++) {
            orderedStops.get(i).setStopOrder(-(i + 1));
        }
        stopRepository.flush();

        for (int i = 0; i < orderedStops.size(); i++) {
            orderedStops.get(i).setStopOrder(i + 1);
        }
    }

    private Route findRouteWithStops(Long routeId) {
        return routeRepository.findWithStopsById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));
    }

    private Stop findStop(Route route, Long stopId) {
        return route.getStops().stream()
                .filter(stop -> stop.getId().equals(stopId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Stop", stopId));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
