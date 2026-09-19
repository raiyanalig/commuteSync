package com.commutesync.trip.service;

import com.commutesync.common.api.PageResponse;
import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.fleet.domain.Driver;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.repository.DriverRepository;
import com.commutesync.fleet.repository.VehicleRepository;
import com.commutesync.route.domain.Route;
import com.commutesync.route.domain.RouteStatus;
import com.commutesync.route.repository.RouteRepository;
import com.commutesync.schedule.domain.Schedule;
import com.commutesync.schedule.domain.ScheduleStatus;
import com.commutesync.schedule.repository.ScheduleRepository;
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.domain.TripStatus;
import com.commutesync.trip.dto.AssignTripRequest;
import com.commutesync.trip.dto.CreateTripRequest;
import com.commutesync.trip.dto.TripResponse;
import com.commutesync.trip.repository.TripRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final ScheduleRepository scheduleRepository;
    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public TripService(TripRepository tripRepository,
                       ScheduleRepository scheduleRepository,
                       RouteRepository routeRepository,
                       DriverRepository driverRepository,
                       VehicleRepository vehicleRepository) {
        this.tripRepository = tripRepository;
        this.scheduleRepository = scheduleRepository;
        this.routeRepository = routeRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public TripResponse create(CreateTripRequest request) {
        Schedule schedule = null;
        Route route;
        LocalDate serviceDate;
        LocalTime departureTime;
        LocalTime arrivalTime;
        Driver driver;
        Vehicle vehicle;

        if (request.scheduleId() != null) {
            schedule = scheduleRepository.findById(request.scheduleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Schedule", request.scheduleId()));
            if (schedule.getStatus() != ScheduleStatus.ACTIVE) {
                throw new BusinessException("Schedule is not active: " + schedule.getId());
            }
            route = schedule.getRoute();
            serviceDate = schedule.getServiceDate();
            departureTime = schedule.getDepartureTime();
            arrivalTime = request.arrivalTime() != null ? request.arrivalTime() : schedule.getArrivalTime();
            driver = request.driverId() != null
                    ? requireAssignableDriver(request.driverId())
                    : assignableOrNull(schedule.getDriver());
            vehicle = request.vehicleId() != null
                    ? requireAssignableVehicle(request.vehicleId())
                    : assignableOrNull(schedule.getVehicle());
        } else {
            if (request.routeId() == null || request.serviceDate() == null
                    || request.departureTime() == null) {
                throw new BusinessException(
                        "routeId, serviceDate and departureTime are required when scheduleId is not provided");
            }
            route = routeRepository.findById(request.routeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route", request.routeId()));
            if (route.getStatus() != RouteStatus.ACTIVE) {
                throw new BusinessException("Route is not active: " + route.getRouteCode());
            }
            serviceDate = request.serviceDate();
            departureTime = request.departureTime();
            arrivalTime = request.arrivalTime();
            driver = request.driverId() != null ? requireAssignableDriver(request.driverId()) : null;
            vehicle = request.vehicleId() != null ? requireAssignableVehicle(request.vehicleId()) : null;
        }

        Trip trip = new Trip();
        trip.setSchedule(schedule);
        trip.setRoute(route);
        trip.setServiceDate(serviceDate);
        trip.setDepartureTime(departureTime);
        trip.setArrivalTime(arrivalTime);
        trip.setDriver(driver);
        trip.setVehicle(vehicle);
        trip.setStatus(driver != null && vehicle != null ? TripStatus.ASSIGNED : TripStatus.SCHEDULED);

        return TripResponse.from(tripRepository.save(trip));
    }

    @Transactional(readOnly = true)
    public PageResponse<TripResponse> getAll(LocalDate serviceDate, TripStatus status,
                                             Long routeId, Long driverId, Long vehicleId,
                                             Pageable pageable) {
        Page<Trip> page = tripRepository.search(serviceDate, status, routeId, driverId, vehicleId, pageable);
        return PageResponse.from(page.map(TripResponse::from));
    }

    @Transactional(readOnly = true)
    public TripResponse getById(Long id) {
        return TripResponse.from(findWithDetails(id));
    }

    @Transactional
    public TripResponse assign(Long id, AssignTripRequest request) {
        Trip trip = findWithDetails(id);
        if (trip.getStatus() != TripStatus.SCHEDULED && trip.getStatus() != TripStatus.ASSIGNED) {
            throw new BusinessException(
                    "Trip can only be assigned before it starts (current status: " + trip.getStatus() + ")");
        }
        Driver driver = requireAssignableDriver(request.driverId());
        Vehicle vehicle = requireAssignableVehicle(request.vehicleId());

        trip.setDriver(driver);
        trip.setVehicle(vehicle);
        if (trip.getStatus() == TripStatus.SCHEDULED) {
            trip.transitionTo(TripStatus.ASSIGNED);
        }
        return TripResponse.from(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse start(Long id) {
        Trip trip = findWithDetails(id);
        requireDriverAndVehicle(trip);
        trip.transitionTo(TripStatus.STARTED);
        trip.setStartedAt(Instant.now());
        return TripResponse.from(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse markInProgress(Long id) {
        Trip trip = findWithDetails(id);
        trip.transitionTo(TripStatus.IN_PROGRESS);
        return TripResponse.from(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse complete(Long id) {
        Trip trip = findWithDetails(id);
        trip.transitionTo(TripStatus.COMPLETED);
        trip.setCompletedAt(Instant.now());
        return TripResponse.from(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse cancel(Long id, String reason) {
        Trip trip = findWithDetails(id);
        trip.transitionTo(TripStatus.CANCELLED);
        trip.setCancelledAt(Instant.now());
        trip.setCancellationReason(trimToNull(reason));
        return TripResponse.from(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse changeStatus(Long id, TripStatus target, String reason) {
        return switch (target) {
            case STARTED -> start(id);
            case IN_PROGRESS -> markInProgress(id);
            case COMPLETED -> complete(id);
            case CANCELLED -> cancel(id, reason);
            case ASSIGNED -> throw new BusinessException(
                    "Use PUT /api/trips/{id}/assignment to assign a driver and vehicle");
            case SCHEDULED -> throw new BusinessException("A trip cannot be moved back to SCHEDULED");
        };
    }

    private Trip findWithDetails(Long id) {
        return tripRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", id));
    }

    private Driver requireAssignableDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));
        if (!driver.isAssignable()) {
            throw new BusinessException("Driver is not available for assignment (status: "
                    + driver.getStatus() + ", available: " + driver.isAvailable() + ")");
        }
        return driver;
    }

    private Vehicle requireAssignableVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));
        if (!vehicle.isAssignable()) {
            throw new BusinessException("Vehicle is not available for assignment (status: "
                    + vehicle.getStatus() + ")");
        }
        return vehicle;
    }

    private Driver assignableOrNull(Driver driver) {
        return driver != null && driver.isAssignable() ? driver : null;
    }

    private Vehicle assignableOrNull(Vehicle vehicle) {
        return vehicle != null && vehicle.isAssignable() ? vehicle : null;
    }

    private void requireDriverAndVehicle(Trip trip) {
        if (trip.getDriver() == null || trip.getVehicle() == null) {
            throw new BusinessException("Trip must have both a driver and a vehicle before it can start");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
