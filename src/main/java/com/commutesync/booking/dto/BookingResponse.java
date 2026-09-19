package com.commutesync.booking.dto;

import com.commutesync.booking.domain.Booking;
import com.commutesync.booking.domain.BookingStatus;
import com.commutesync.employee.domain.Employee;
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.domain.TripStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record BookingResponse(
        Long id,
        BookingStatus status,
        Integer seatNumber,
        Instant bookedAt,
        Instant cancelledAt,
        String cancellationReason,
        EmployeeRef employee,
        TripRef trip,
        Instant createdAt,
        Instant updatedAt
) {

    public record EmployeeRef(Long id, String employeeCode, String fullName, String email) {
    }

    public record TripRef(Long id, LocalDate serviceDate, LocalTime departureTime,
                          TripStatus status, String routeCode, String routeName) {
    }

    public static BookingResponse from(Booking booking) {
        Employee employee = booking.getEmployee();
        Trip trip = booking.getTrip();

        return new BookingResponse(
                booking.getId(),
                booking.getStatus(),
                booking.getSeatNumber(),
                booking.getBookedAt(),
                booking.getCancelledAt(),
                booking.getCancellationReason(),
                new EmployeeRef(employee.getId(), employee.getEmployeeCode(),
                        employee.getFullName(), employee.getEmail()),
                new TripRef(trip.getId(), trip.getServiceDate(), trip.getDepartureTime(),
                        trip.getStatus(), trip.getRoute().getRouteCode(), trip.getRoute().getName()),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
