package com.commutesync.admin.dto;

public record FleetSummaryResponse(
        long totalVehicles,
        long availableVehicles,
        long vehiclesInUse,
        long vehiclesInMaintenance,
        long vehiclesInactive,
        long totalDrivers,
        long activeDrivers,
        long availableDrivers
) {
}
