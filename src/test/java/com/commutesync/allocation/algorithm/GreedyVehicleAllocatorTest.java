package com.commutesync.allocation.algorithm;

import static org.assertj.core.api.Assertions.assertThat;

import com.commutesync.allocation.algorithm.AllocationPlan.PlannedVehicle;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.domain.VehicleType;
import java.util.List;
import org.junit.jupiter.api.Test;

class GreedyVehicleAllocatorTest {

    private final GreedyVehicleAllocator allocator = new GreedyVehicleAllocator();

    @Test
    void noDemandProducesEmptyPlan() {
        AllocationPlan plan = allocator.allocate(0, List.of(vehicle(1L, "A", 12)));

        assertThat(plan.allocations()).isEmpty();
        assertThat(plan.allocatedSeats()).isZero();
        assertThat(plan.unallocatedDemand()).isZero();
        assertThat(plan.fullyAllocated()).isTrue();
    }

    @Test
    void negativeDemandIsTreatedAsNoDemand() {
        AllocationPlan plan = allocator.allocate(-5, List.of(vehicle(1L, "A", 12)));

        assertThat(plan.allocations()).isEmpty();
        assertThat(plan.fullyAllocated()).isTrue();
    }

    @Test
    void demandFitsInOneVehicleWithExcessCapacity() {
        AllocationPlan plan = allocator.allocate(7, List.of(vehicle(1L, "A", 12)));

        assertThat(plan.allocations()).hasSize(1);
        assertThat(plan.allocations().get(0).seats()).isEqualTo(7);
        assertThat(plan.allocatedSeats()).isEqualTo(7);
        assertThat(plan.fullyAllocated()).isTrue();
    }

    @Test
    void demandExactlyMatchesVehicleCapacity() {
        AllocationPlan plan = allocator.allocate(12, List.of(vehicle(1L, "A", 12)));

        assertThat(plan.allocations()).hasSize(1);
        assertThat(plan.allocations().get(0).seats()).isEqualTo(12);
        assertThat(plan.unallocatedDemand()).isZero();
    }

    @Test
    void demandSplitsAcrossLargestVehiclesFirst() {
        List<Vehicle> vehicles = List.of(
                vehicle(1L, "SMALL", 6),
                vehicle(2L, "BIG", 20),
                vehicle(3L, "MEDIUM", 12));

        AllocationPlan plan = allocator.allocate(30, vehicles);

        assertThat(plan.allocations()).extracting(a -> a.vehicle().getRegistrationNumber())
                .containsExactly("BIG", "MEDIUM");
        assertThat(plan.allocations()).extracting(PlannedVehicle::seats).containsExactly(20, 10);
        assertThat(plan.allocatedSeats()).isEqualTo(30);
        assertThat(plan.fullyAllocated()).isTrue();
    }

    @Test
    void doesNotAllocateMoreVehiclesThanNeeded() {
        List<Vehicle> vehicles = List.of(
                vehicle(1L, "A", 12), vehicle(2L, "B", 12), vehicle(3L, "C", 12));

        AllocationPlan plan = allocator.allocate(15, vehicles);

        assertThat(plan.allocations()).hasSize(2);
        assertThat(plan.allocations()).extracting(PlannedVehicle::seats).containsExactly(12, 3);
        assertThat(plan.allocatedSeats()).isEqualTo(15);
    }

    @Test
    void insufficientCapacityReportsShortfall() {
        List<Vehicle> vehicles = List.of(vehicle(1L, "A", 12), vehicle(2L, "B", 12));

        AllocationPlan plan = allocator.allocate(30, vehicles);

        assertThat(plan.allocations()).hasSize(2);
        assertThat(plan.allocatedSeats()).isEqualTo(24);
        assertThat(plan.unallocatedDemand()).isEqualTo(6);
        assertThat(plan.fullyAllocated()).isFalse();
    }

    @Test
    void noVehiclesLeavesAllDemandUnallocated() {
        AllocationPlan plan = allocator.allocate(10, List.of());

        assertThat(plan.allocations()).isEmpty();
        assertThat(plan.allocatedSeats()).isZero();
        assertThat(plan.unallocatedDemand()).isEqualTo(10);
        assertThat(plan.fullyAllocated()).isFalse();
    }

    @Test
    void ignoresVehiclesWithNonPositiveCapacity() {
        List<Vehicle> vehicles = List.of(
                vehicle(1L, "ZERO", 0),
                vehicle(2L, "GOOD", 10));

        AllocationPlan plan = allocator.allocate(5, vehicles);

        assertThat(plan.allocations()).hasSize(1);
        assertThat(plan.allocations().get(0).vehicle().getRegistrationNumber()).isEqualTo("GOOD");
    }

    @Test
    void tieBreakIsDeterministicByRegistration() {
        List<Vehicle> vehicles = List.of(
                vehicle(1L, "PB10ZZ9999", 12),
                vehicle(2L, "PB10AA0001", 12));

        AllocationPlan plan = allocator.allocate(24, vehicles);

        assertThat(plan.allocations()).extracting(a -> a.vehicle().getRegistrationNumber())
                .containsExactly("PB10AA0001", "PB10ZZ9999");
    }

    @Test
    void singlePassengerUsesTheLargestVehicle() {
        List<Vehicle> vehicles = List.of(vehicle(1L, "VAN", 12), vehicle(2L, "BUS", 50));

        AllocationPlan plan = allocator.allocate(1, vehicles);

        assertThat(plan.allocations()).hasSize(1);
        assertThat(plan.allocations().get(0).vehicle().getRegistrationNumber()).isEqualTo("BUS");
        assertThat(plan.allocations().get(0).seats()).isEqualTo(1);
    }

    private Vehicle vehicle(Long id, String registrationNumber, int capacity) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setRegistrationNumber(registrationNumber);
        vehicle.setType(VehicleType.VAN);
        vehicle.setCapacity(capacity);
        return vehicle;
    }
}
