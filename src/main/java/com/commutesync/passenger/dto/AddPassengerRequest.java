package com.commutesync.passenger.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddPassengerRequest(

        @NotNull(message = "Booking id is required")
        @Positive(message = "Booking id must be positive")
        Long bookingId
) {
}
