package com.commutesync.trip.dto;

import jakarta.validation.constraints.Size;

public record CancelTripRequest(

        @Size(max = 255, message = "Reason must be at most 255 characters")
        String reason
) {
}
