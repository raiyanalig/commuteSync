package com.commutesync.allocation.algorithm;

import com.commutesync.allocation.algorithm.AllocationPlan.PlannedVehicle;
import com.commutesync.fleet.domain.Vehicle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Capacity-based greedy allocator.
 *
 * <p>Algorithm (First-Fit Decreasing style):
 * <ol>
 *   <li>Discard vehicles with non-positive capacity.</li>
 *   <li>Sort remaining vehicles by capacity descending (largest first). Ties broken by
 *       registration number so the result is deterministic and testable.</li>
 *   <li>Walk the sorted list, filling each vehicle with {@code min(remaining, capacity)}
 *       passengers and subtracting from the remaining demand.</li>
 *   <li>Stop as soon as demand is satisfied (never allocate more than needed).</li>
 * </ol>
 *
 * <p>Packing the largest vehicles first minimises the number of vehicles used — the
 * classic bin-packing heuristic — while keeping the implementation linear after sorting.
 *
 * <p>Time complexity: O(V log V) for the sort plus O(V) for the pass, where V is the
 * number of candidate vehicles. Space: O(V).
 */
@Component
public class GreedyVehicleAllocator {

    private static final Comparator<Vehicle> BY_CAPACITY_DESC_THEN_REGISTRATION =
            Comparator.comparingInt(Vehicle::getCapacity)
                    .reversed()
                    .thenComparing(Vehicle::getRegistrationNumber,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    public AllocationPlan allocate(int demand, List<Vehicle> candidates) {
        if (demand <= 0) {
            return AllocationPlan.empty(demand);
        }

        List<Vehicle> ordered = candidates.stream()
                .filter(vehicle -> vehicle.getCapacity() > 0)
                .sorted(BY_CAPACITY_DESC_THEN_REGISTRATION)
                .toList();

        List<PlannedVehicle> allocations = new ArrayList<>();
        int remaining = demand;

        for (Vehicle vehicle : ordered) {
            if (remaining == 0) {
                break;
            }
            int seats = Math.min(remaining, vehicle.getCapacity());
            allocations.add(new PlannedVehicle(vehicle, seats));
            remaining -= seats;
        }

        return new AllocationPlan(allocations, demand - remaining, remaining);
    }
}
