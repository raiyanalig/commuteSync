package com.commutesync.fleet.dto;

import com.commutesync.fleet.domain.Driver;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.domain.VehicleStatus;
import com.commutesync.fleet.domain.VehicleType;
import java.time.Instant;

public record VehicleResponse(
        Long id,
        String registrationNumber,
        VehicleType type,
        String model,
        int capacity,
        VehicleStatus status,
        DriverSummary assignedDriver,
        boolean assignable,
        Instant createdAt,
        Instant updatedAt
) {

    public static VehicleResponse from(Vehicle vehicle) {
        Driver driver = vehicle.getAssignedDriver();
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getType(),
                vehicle.getModel(),
                vehicle.getCapacity(),
                vehicle.getStatus(),
                driver == null ? null : DriverSummary.from(driver),
                vehicle.isAssignable(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }
}
