package com.commutesync.schedule.dto;

import com.commutesync.schedule.domain.ScheduleStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateScheduleRequest(

        @NotNull(message = "Shift id is required")
        @Positive(message = "Shift id must be positive")
        Long shiftId,

        @NotNull(message = "Route id is required")
        @Positive(message = "Route id must be positive")
        Long routeId,

        @NotNull(message = "Service date is required")
        @FutureOrPresent(message = "Service date must be today or in the future")
        LocalDate serviceDate,

        @NotNull(message = "Departure time is required")
        LocalTime departureTime,

        LocalTime arrivalTime,

        @Positive(message = "Vehicle id must be positive")
        Long vehicleId,

        @Positive(message = "Driver id must be positive")
        Long driverId,

        ScheduleStatus status
) {
}
