package com.commutesync.allocation.dto;

import com.commutesync.allocation.algorithm.AllocationPlan;
import com.commutesync.allocation.domain.VehicleAssignment;
import com.commutesync.schedule.domain.Schedule;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AllocationResponse(
        Long scheduleId,
        String routeCode,
        String routeName,
        LocalDate serviceDate,
        LocalTime departureTime,
        int demand,
        int allocatedSeats,
        int unallocatedDemand,
        boolean fullyAllocated,
        List<VehicleAssignmentResponse> assignments
) {

    public static AllocationResponse from(Schedule schedule, int demand, AllocationPlan plan,
                                          List<VehicleAssignment> assignments) {
        return new AllocationResponse(
                schedule.getId(),
                schedule.getRoute().getRouteCode(),
                schedule.getRoute().getName(),
                schedule.getServiceDate(),
                schedule.getDepartureTime(),
                demand,
                plan.allocatedSeats(),
                plan.unallocatedDemand(),
                plan.fullyAllocated(),
                assignments.stream().map(VehicleAssignmentResponse::from).toList()
        );
    }
}
