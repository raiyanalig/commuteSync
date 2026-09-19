package com.commutesync.trip.dto;

import com.commutesync.fleet.domain.Driver;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.route.domain.Route;
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.domain.TripStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record TripResponse(
        Long id,
        LocalDate serviceDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        TripStatus status,
        Instant startedAt,
        Instant completedAt,
        Instant cancelledAt,
        String cancellationReason,
        Long scheduleId,
        RouteRef route,
        DriverRef driver,
        VehicleRef vehicle,
        Instant createdAt,
        Instant updatedAt
) {

    public record RouteRef(Long id, String routeCode, String name,
                           String source, String destination) {
    }

    public record DriverRef(Long id, String fullName, String phone) {
    }

    public record VehicleRef(Long id, String registrationNumber, int capacity) {
    }

    public static TripResponse from(Trip trip) {
        Route route = trip.getRoute();
        Driver driver = trip.getDriver();
        Vehicle vehicle = trip.getVehicle();

        return new TripResponse(
                trip.getId(),
                trip.getServiceDate(),
                trip.getDepartureTime(),
                trip.getArrivalTime(),
                trip.getStatus(),
                trip.getStartedAt(),
                trip.getCompletedAt(),
                trip.getCancelledAt(),
                trip.getCancellationReason(),
                trip.getSchedule() == null ? null : trip.getSchedule().getId(),
                new RouteRef(route.getId(), route.getRouteCode(), route.getName(),
                        route.getSource(), route.getDestination()),
                driver == null ? null
                        : new DriverRef(driver.getId(), driver.getFullName(), driver.getPhone()),
                vehicle == null ? null
                        : new VehicleRef(vehicle.getId(), vehicle.getRegistrationNumber(), vehicle.getCapacity()),
                trip.getCreatedAt(),
                trip.getUpdatedAt()
        );
    }
}
