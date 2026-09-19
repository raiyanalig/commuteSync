package com.commutesync.allocation.service;

import com.commutesync.allocation.algorithm.AllocationPlan;
import com.commutesync.allocation.algorithm.GreedyVehicleAllocator;
import com.commutesync.allocation.domain.VehicleAssignment;
import com.commutesync.allocation.dto.AllocationRequest;
import com.commutesync.allocation.dto.AllocationResponse;
import com.commutesync.allocation.dto.VehicleAssignmentResponse;
import com.commutesync.allocation.repository.VehicleAssignmentRepository;
import com.commutesync.booking.domain.BookingStatus;
import com.commutesync.booking.repository.BookingRepository;
import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.domain.VehicleStatus;
import com.commutesync.fleet.repository.VehicleRepository;
import com.commutesync.schedule.domain.Schedule;
import com.commutesync.schedule.domain.ScheduleStatus;
import com.commutesync.schedule.repository.ScheduleRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AllocationService {

    private final GreedyVehicleAllocator allocator;
    private final ScheduleRepository scheduleRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final VehicleAssignmentRepository assignmentRepository;

    public AllocationService(GreedyVehicleAllocator allocator,
                             ScheduleRepository scheduleRepository,
                             VehicleRepository vehicleRepository,
                             BookingRepository bookingRepository,
                             VehicleAssignmentRepository assignmentRepository) {
        this.allocator = allocator;
        this.scheduleRepository = scheduleRepository;
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.assignmentRepository = assignmentRepository;
    }

    /**
     * Runs the allocation for a schedule. Any previous plan for that schedule is
     * discarded first, so re-running recomputes from the current demand and fleet.
     */
    @Transactional
    public AllocationResponse allocate(AllocationRequest request) {
        Schedule schedule = scheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", request.scheduleId()));
        if (schedule.getStatus() != ScheduleStatus.ACTIVE) {
            throw new BusinessException("Schedule is not active: " + schedule.getId());
        }

        int demand = request.passengerCount() != null
                ? request.passengerCount()
                : (int) bookingRepository.countConfirmedByScheduleId(
                        schedule.getId(), BookingStatus.CONFIRMED);

        assignmentRepository.deleteByScheduleId(schedule.getId());

        List<Vehicle> candidates = vehicleRepository
                .findByStatusAndCapacityGreaterThanOrderByCapacityDesc(VehicleStatus.AVAILABLE, 0);
        AllocationPlan plan = allocator.allocate(demand, candidates);

        List<VehicleAssignment> saved = new ArrayList<>();
        for (AllocationPlan.PlannedVehicle planned : plan.allocations()) {
            VehicleAssignment assignment = new VehicleAssignment();
            assignment.setSchedule(schedule);
            assignment.setVehicle(planned.vehicle());
            assignment.setPassengerCount(planned.seats());
            assignment.setAllocatedCapacity(planned.vehicle().getCapacity());
            saved.add(assignmentRepository.save(assignment));
        }

        return AllocationResponse.from(schedule, demand, plan, saved);
    }

    @Transactional(readOnly = true)
    public List<VehicleAssignmentResponse> getAssignments(Long scheduleId) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ResourceNotFoundException("Schedule", scheduleId);
        }
        return assignmentRepository.findByScheduleId(scheduleId).stream()
                .map(VehicleAssignmentResponse::from)
                .toList();
    }
}
