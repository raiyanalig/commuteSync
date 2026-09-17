package com.commutesync.fleet.dto;

import com.commutesync.fleet.domain.VehicleStatus;
import com.commutesync.fleet.domain.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateVehicleRequest(

        @NotBlank(message = "Registration number is required")
        @Pattern(regexp = "^[A-Za-z0-9 -]{4,20}$",
                message = "Registration number must be 4-20 characters (letters, digits, space, hyphen)")
        String registrationNumber,

        @NotNull(message = "Vehicle type is required")
        VehicleType type,

        @Size(max = 100, message = "Model must be at most 100 characters")
        String model,

        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        @Max(value = 60, message = "Capacity must be at most 60")
        Integer capacity,

        VehicleStatus status
) {
}
