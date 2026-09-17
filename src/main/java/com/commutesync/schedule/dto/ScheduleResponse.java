package com.commutesync.schedule.dto;

import com.commutesync.fleet.domain.Driver;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.route.domain.Route;
import com.commutesync.schedule.domain.Schedule;
import com.commutesync.schedule.domain.ScheduleStatus;
import com.commutesync.schedule.domain.Shift;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleResponse(
        Long id,
        LocalDate serviceDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        ScheduleStatus status,
        ShiftRef shift,
        RouteRef route,
        VehicleRef vehicle,
        DriverRef driver,
        Instant createdAt,
        Instant updatedAt
) {

    public record ShiftRef(Long id, String shiftCode, String name,
                           LocalTime startTime, LocalTime endTime) {
    }

    public record RouteRef(Long id, String routeCode, String name,
                           String source, String destination) {
    }

    public record VehicleRef(Long id, String registrationNumber, int capacity) {
    }

    public record DriverRef(Long id, String fullName, String phone) {
    }

    public static ScheduleResponse from(Schedule schedule) {
        Shift shift = schedule.getShift();
        Route route = schedule.getRoute();
        Vehicle vehicle = schedule.getVehicle();
        Driver driver = schedule.getDriver();

        return new ScheduleResponse(
                schedule.getId(),
                schedule.getServiceDate(),
                schedule.getDepartureTime(),
                schedule.getArrivalTime(),
                schedule.getStatus(),
                new ShiftRef(shift.getId(), shift.getShiftCode(), shift.getName(),
                        shift.getStartTime(), shift.getEndTime()),
                new RouteRef(route.getId(), route.getRouteCode(), route.getName(),
                        route.getSource(), route.getDestination()),
                vehicle == null ? null
                        : new VehicleRef(vehicle.getId(), vehicle.getRegistrationNumber(), vehicle.getCapacity()),
                driver == null ? null
                        : new DriverRef(driver.getId(), driver.getFullName(), driver.getPhone()),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
