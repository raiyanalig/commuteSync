package com.commutesync.tracking.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;

public record TrackingUpdateRequest(

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        @Digits(integer = 3, fraction = 6, message = "Latitude must have at most 6 decimal places")
        BigDecimal latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        @Digits(integer = 3, fraction = 6, message = "Longitude must have at most 6 decimal places")
        BigDecimal longitude,

        @PositiveOrZero(message = "Speed must not be negative")
        @Digits(integer = 3, fraction = 2, message = "Speed must have at most 2 decimal places")
        BigDecimal speedKph,

        /** Optional device timestamp; defaults to server time when omitted. */
        Instant recordedAt
) {
}
