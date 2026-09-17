package com.commutesync.fleet.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateDriverAvailabilityRequest(

        @NotNull(message = "Availability is required")
        Boolean available
) {
}
