package com.commutesync.tracking.dto;

import com.commutesync.tracking.domain.TripLocation;
import com.commutesync.trip.domain.TripStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TripLocationResponse(
        Long id,
        Long tripId,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal speedKph,
        TripStatus tripStatus,
        Instant recordedAt,
        Instant createdAt
) {

    public static TripLocationResponse from(TripLocation location) {
        return new TripLocationResponse(
                location.getId(),
                location.getTrip().getId(),
                location.getLatitude(),
                location.getLongitude(),
                location.getSpeedKph(),
                location.getTripStatus(),
                location.getRecordedAt(),
                location.getCreatedAt()
        );
    }
}
