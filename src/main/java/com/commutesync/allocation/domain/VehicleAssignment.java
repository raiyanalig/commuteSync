package com.commutesync.allocation.domain;

import com.commutesync.common.persistence.BaseEntity;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.schedule.domain.Schedule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Persisted output of the allocation engine: one row per vehicle chosen to serve a
 * schedule, together with how many confirmed passengers it carries. The unique
 * constraint stops the same vehicle being assigned twice to one schedule.
 */
@Entity
@Table(name = "vehicle_assignments", uniqueConstraints =
        @UniqueConstraint(name = "uk_vehicle_assignments_schedule_vehicle",
                columnNames = {"schedule_id", "vehicle_id"}))
public class VehicleAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "passenger_count", nullable = false)
    private int passengerCount;

    @Column(name = "allocated_capacity", nullable = false)
    private int allocatedCapacity;

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(int passengerCount) {
        this.passengerCount = passengerCount;
    }

    public int getAllocatedCapacity() {
        return allocatedCapacity;
    }

    public void setAllocatedCapacity(int allocatedCapacity) {
        this.allocatedCapacity = allocatedCapacity;
    }
}
