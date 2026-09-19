package com.commutesync.trip.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignTripRequest(

        @NotNull(message = "Driver id is required")
        @Positive(message = "Driver id must be positive")
        Long driverId,

        @NotNull(message = "Vehicle id is required")
        @Positive(message = "Vehicle id must be positive")
        Long vehicleId
) {
}
