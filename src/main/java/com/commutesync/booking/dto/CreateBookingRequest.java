package com.commutesync.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateBookingRequest(

        @NotNull(message = "Trip id is required")
        @Positive(message = "Trip id must be positive")
        Long tripId
) {
}
