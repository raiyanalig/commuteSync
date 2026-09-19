package com.commutesync.allocation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record AllocationRequest(

        @NotNull(message = "Schedule id is required")
        @Positive(message = "Schedule id must be positive")
        Long scheduleId,

        /**
         * Optional explicit demand. When omitted, demand is derived from the number of
         * CONFIRMED bookings across the schedule's trips.
         */
        @PositiveOrZero(message = "Passenger count must not be negative")
        Integer passengerCount
) {
}
