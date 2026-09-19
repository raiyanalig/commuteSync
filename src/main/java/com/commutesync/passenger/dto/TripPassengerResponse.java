package com.commutesync.passenger.dto;

import com.commutesync.employee.domain.Employee;
import com.commutesync.passenger.domain.PassengerStatus;
import com.commutesync.passenger.domain.TripPassenger;
import java.time.Instant;

public record TripPassengerResponse(
        Long id,
        Long tripId,
        Long bookingId,
        Long employeeId,
        String employeeCode,
        String fullName,
        String email,
        Integer seatNumber,
        PassengerStatus status,
        Instant confirmedAt,
        Instant boardedAt,
        Instant createdAt
) {

    public static TripPassengerResponse from(TripPassenger passenger) {
        Employee employee = passenger.getEmployee();
        return new TripPassengerResponse(
                passenger.getId(),
                passenger.getTrip().getId(),
                passenger.getBooking().getId(),
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getEmail(),
                passenger.getSeatNumber(),
                passenger.getStatus(),
                passenger.getConfirmedAt(),
                passenger.getBoardedAt(),
                passenger.getCreatedAt()
        );
    }
}
