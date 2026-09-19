package com.commutesync.passenger.domain;

import com.commutesync.booking.domain.Booking;
import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.persistence.BaseEntity;
import com.commutesync.employee.domain.Employee;
import com.commutesync.trip.domain.Trip;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Operational manifest entry for one passenger on one trip. It is the association
 * entity between {@link Trip} and {@link Employee}, carrying attributes that a plain
 * many-to-many join cannot hold: the seat, the boarding lifecycle and the link back
 * to the commercial {@link Booking}.
 */
@Entity
@Table(name = "trip_passengers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_trip_passengers_trip_employee",
                columnNames = {"trip_id", "employee_id"}),
        @UniqueConstraint(name = "uk_trip_passengers_trip_seat",
                columnNames = {"trip_id", "seat_number"})
})
public class TripPassenger extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "seat_number")
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PassengerStatus status = PassengerStatus.EXPECTED;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "boarded_at")
    private Instant boardedAt;

    public void transitionTo(PassengerStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessException("Invalid passenger state transition: " + status + " -> " + target);
        }
        this.status = target;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public PassengerStatus getStatus() {
        return status;
    }

    public void setStatus(PassengerStatus status) {
        this.status = status;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getBoardedAt() {
        return boardedAt;
    }

    public void setBoardedAt(Instant boardedAt) {
        this.boardedAt = boardedAt;
    }
}
