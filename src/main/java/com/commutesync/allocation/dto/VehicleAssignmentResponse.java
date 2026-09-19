package com.commutesync.allocation.dto;

import com.commutesync.allocation.domain.VehicleAssignment;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.domain.VehicleType;
import java.time.Instant;

public record VehicleAssignmentResponse(
        Long id,
        Long scheduleId,
        Long vehicleId,
        String registrationNumber,
        VehicleType vehicleType,
        int vehicleCapacity,
        int passengerCount,
        int allocatedCapacity,
        Instant createdAt
) {

    public static VehicleAssignmentResponse from(VehicleAssignment assignment) {
        Vehicle vehicle = assignment.getVehicle();
        return new VehicleAssignmentResponse(
                assignment.getId(),
                assignment.getSchedule().getId(),
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getType(),
                vehicle.getCapacity(),
                assignment.getPassengerCount(),
                assignment.getAllocatedCapacity(),
                assignment.getCreatedAt()
        );
    }
}
