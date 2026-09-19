package com.commutesync.trip.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateTripRequest(

        @Positive(message = "Schedule id must be positive")
        Long scheduleId,

        @Positive(message = "Route id must be positive")
        Long routeId,

        @FutureOrPresent(message = "Service date must be today or in the future")
        LocalDate serviceDate,

        LocalTime departureTime,

        LocalTime arrivalTime,

        @Positive(message = "Driver id must be positive")
        Long driverId,

        @Positive(message = "Vehicle id must be positive")
        Long vehicleId
) {

    @AssertTrue(message = "routeId, serviceDate and departureTime are required when scheduleId is not provided")
    public boolean isStandaloneDetailsValid() {
        return scheduleId != null
                || (routeId != null && serviceDate != null && departureTime != null);
    }

    @AssertTrue(message = "driverId and vehicleId must be provided together")
    public boolean isAssignmentConsistent() {
        return (driverId == null) == (vehicleId == null);
    }
}
