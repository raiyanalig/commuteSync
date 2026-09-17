package com.commutesync.route.dto;

import com.commutesync.route.domain.RouteStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateRouteRequest(

        @NotBlank(message = "Route code is required")
        @Pattern(regexp = "^[A-Za-z0-9-]{2,20}$",
                message = "Route code must be 2-20 characters (letters, digits, hyphen)")
        String routeCode,

        @NotBlank(message = "Route name is required")
        @Size(max = 150, message = "Route name must be at most 150 characters")
        String name,

        @NotBlank(message = "Source is required")
        @Size(max = 150, message = "Source must be at most 150 characters")
        String source,

        @NotBlank(message = "Destination is required")
        @Size(max = 150, message = "Destination must be at most 150 characters")
        String destination,

        @Positive(message = "Distance must be positive")
        @Digits(integer = 4, fraction = 2, message = "Distance must have at most 4 integer and 2 decimal digits")
        BigDecimal distanceKm,

        @Positive(message = "Estimated duration must be positive")
        @Max(value = 1440, message = "Estimated duration must be at most 1440 minutes")
        Integer estimatedDurationMinutes,

        @NotNull(message = "Status is required")
        RouteStatus status
) {
}
