package com.commutesync.trip.dto;

import com.commutesync.trip.domain.TripStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTripStatusRequest(

        @NotNull(message = "Status is required")
        TripStatus status,

        @Size(max = 255, message = "Reason must be at most 255 characters")
        String reason
) {
}
