package com.commutesync.schedule.dto;

import com.commutesync.schedule.domain.ScheduleStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateScheduleRequest(

        @NotNull(message = "Shift id is required")
        @Positive(message = "Shift id must be positive")
        Long shiftId,

        @NotNull(message = "Route id is required")
        @Positive(message = "Route id must be positive")
        Long routeId,

        @NotNull(message = "Service date is required")
        LocalDate serviceDate,

        @NotNull(message = "Departure time is required")
        LocalTime departureTime,

        LocalTime arrivalTime,

        @Positive(message = "Vehicle id must be positive")
        Long vehicleId,

        @Positive(message = "Driver id must be positive")
        Long driverId,

        @NotNull(message = "Status is required")
        ScheduleStatus status
) {
}
