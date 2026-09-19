package com.commutesync.admin.dto;

import java.time.Instant;

public record DashboardResponse(
        long totalEmployees,
        long totalDrivers,
        long availableDrivers,
        long totalVehicles,
        long availableVehicles,
        long activeTrips,
        long upcomingTrips,
        long totalBookings,
        long confirmedBookings,
        long cancelledBookings,
        Instant generatedAt
) {
}
