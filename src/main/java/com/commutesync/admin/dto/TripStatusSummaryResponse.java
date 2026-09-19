package com.commutesync.admin.dto;

import com.commutesync.trip.domain.TripStatus;
import java.time.LocalDate;
import java.util.Map;

public record TripStatusSummaryResponse(
        LocalDate serviceDate,
        long total,
        Map<TripStatus, Long> byStatus
) {
}
