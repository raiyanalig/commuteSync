package com.commutesync.booking.dto;

import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.route.domain.Route;
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.domain.TripStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public record AvailableTripResponse(
        Long tripId,
        LocalDate serviceDate,
        LocalTime departureTime,
        TripStatus status,
        Long routeId,
        String routeCode,
        String routeName,
        String source,
        String destination,
        Long vehicleId,
        String registrationNumber,
        int capacity,
        long confirmedBookings,
        long availableSeats
) {

    public static AvailableTripResponse of(Trip trip, long confirmedBookings) {
        Route route = trip.getRoute();
        Vehicle vehicle = trip.getVehicle();
        int capacity = vehicle.getCapacity();

        return new AvailableTripResponse(
                trip.getId(),
                trip.getServiceDate(),
                trip.getDepartureTime(),
                trip.getStatus(),
                route.getId(),
                route.getRouteCode(),
                route.getName(),
                route.getSource(),
                route.getDestination(),
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                capacity,
                confirmedBookings,
                Math.max(0, capacity - confirmedBookings)
        );
    }
}
