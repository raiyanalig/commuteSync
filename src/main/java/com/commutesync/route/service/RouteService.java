package com.commutesync.route.service;

import com.commutesync.common.api.PageResponse;
import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.DuplicateResourceException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.route.domain.Route;
import com.commutesync.route.domain.RouteStatus;
import com.commutesync.route.domain.Stop;
import com.commutesync.route.dto.CreateRouteRequest;
import com.commutesync.route.dto.CreateStopRequest;
import com.commutesync.route.dto.RouteResponse;
import com.commutesync.route.dto.RouteSummaryResponse;
import com.commutesync.route.dto.UpdateRouteRequest;
import com.commutesync.route.repository.RouteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RouteService {

    private final RouteRepository routeRepository;

    public RouteService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Transactional
    public RouteResponse create(CreateRouteRequest request) {
        String routeCode = normalizeCode(request.routeCode());
        if (routeRepository.existsByRouteCode(routeCode)) {
            throw new DuplicateResourceException("Route code already exists: " + routeCode);
        }
        ensureDistinctEndpoints(request.source(), request.destination());

        Route route = new Route();
        route.setRouteCode(routeCode);
        route.setName(request.name().trim());
        route.setSource(request.source().trim());
        route.setDestination(request.destination().trim());
        route.setDistanceKm(request.distanceKm());
        route.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        route.setStatus(request.status() == null ? RouteStatus.ACTIVE : request.status());

        if (request.stops() != null) {
            int order = 1;
            for (CreateStopRequest stopRequest : request.stops()) {
                Stop stop = new Stop();
                stop.setName(stopRequest.name().trim());
                stop.setAddress(trimToNull(stopRequest.address()));
                stop.setStopOrder(order++);
                route.addStop(stop);
            }
        }

        return RouteResponse.from(routeRepository.save(route));
    }

    @Transactional(readOnly = true)
    public PageResponse<RouteSummaryResponse> getAll(RouteStatus status, String search, Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        Page<Route> page = routeRepository.search(status, normalizedSearch, pageable);
        return PageResponse.from(page.map(RouteSummaryResponse::from));
    }

    @Transactional(readOnly = true)
    public RouteResponse getById(Long id) {
        return RouteResponse.from(findWithStops(id));
    }

    @Transactional
    public RouteResponse update(Long id, UpdateRouteRequest request) {
        Route route = findWithStops(id);
        String routeCode = normalizeCode(request.routeCode());

        if (!route.getRouteCode().equals(routeCode) && routeRepository.existsByRouteCode(routeCode)) {
            throw new DuplicateResourceException("Route code already exists: " + routeCode);
        }
        ensureDistinctEndpoints(request.source(), request.destination());

        route.setRouteCode(routeCode);
        route.setName(request.name().trim());
        route.setSource(request.source().trim());
        route.setDestination(request.destination().trim());
        route.setDistanceKm(request.distanceKm());
        route.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        route.setStatus(request.status());

        return RouteResponse.from(routeRepository.save(route));
    }

    @Transactional
    public void delete(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", id));
        routeRepository.delete(route);
    }

    private Route findWithStops(Long id) {
        return routeRepository.findWithStopsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", id));
    }

    private void ensureDistinctEndpoints(String source, String destination) {
        if (source.trim().equalsIgnoreCase(destination.trim())) {
            throw new BusinessException("Source and destination must be different");
        }
    }

    private String normalizeCode(String routeCode) {
        return routeCode.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
