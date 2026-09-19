package com.commutesync.booking.dto;

import jakarta.validation.constraints.Size;

public record CancelBookingRequest(

        @Size(max = 255, message = "Reason must be at most 255 characters")
        String reason
) {
}
