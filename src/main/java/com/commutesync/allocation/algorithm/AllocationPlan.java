package com.commutesync.allocation.algorithm;

import com.commutesync.fleet.domain.Vehicle;
import java.util.List;

/**
 * Result of running the allocation algorithm. Pure data: no persistence, no Spring.
 */
public record AllocationPlan(
        List<PlannedVehicle> allocations,
        int allocatedSeats,
        int unallocatedDemand
) {

    public record PlannedVehicle(Vehicle vehicle, int seats) {
    }

    public boolean fullyAllocated() {
        return unallocatedDemand == 0;
    }

    public static AllocationPlan empty(int demand) {
        return new AllocationPlan(List.of(), 0, Math.max(0, demand));
    }
}
