package com.commutesync.fleet.dto;

import com.commutesync.fleet.domain.VehicleStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateVehicleStatusRequest(

        @NotNull(message = "Status is required")
        VehicleStatus status
) {
}
