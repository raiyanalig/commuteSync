package com.commutesync.passenger.service;

import com.commutesync.booking.domain.Booking;
import com.commutesync.booking.domain.BookingStatus;
import com.commutesync.booking.repository.BookingRepository;
import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.passenger.domain.PassengerStatus;
import com.commutesync.passenger.domain.TripPassenger;
import com.commutesync.passenger.dto.PassengerSummaryResponse;
import com.commutesync.passenger.dto.TripPassengerResponse;
import com.commutesync.passenger.repository.TripPassengerRepository;
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.repository.TripRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripPassengerService {

    private final TripPassengerRepository passengerRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;

    public TripPassengerService(TripPassengerRepository passengerRepository,
                                TripRepository tripRepository,
                                BookingRepository bookingRepository) {
        this.passengerRepository = passengerRepository;
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public List<TripPassengerResponse> getPassengers(Long tripId) {
        Trip trip = findTrip(tripId);
        syncFromBookings(trip);
        return passengerRepository
                .findByTripIdAndStatusNotOrderBySeatNumberAsc(tripId, PassengerStatus.CANCELLED)
                .stream()
                .map(TripPassengerResponse::from)
                .toList();
    }

    @Transactional
    public PassengerSummaryResponse getSummary(Long tripId) {
        Trip trip = findTrip(tripId);
        syncFromBookings(trip);

        List<TripPassenger> active = passengerRepository
                .findByTripIdAndStatusNot(tripId, PassengerStatus.CANCELLED);
        int capacity = trip.getVehicle() == null ? 0 : trip.getVehicle().getCapacity();
        int confirmed = (int) active.stream()
                .filter(p -> p.getStatus() == PassengerStatus.CONFIRMED).count();
        int boarded = (int) active.stream()
                .filter(p -> p.getStatus() == PassengerStatus.BOARDED).count();

        return new PassengerSummaryResponse(
                tripId, capacity, active.size(), confirmed, boarded,
                Math.max(0, capacity - active.size()));
    }

    @Transactional
    public TripPassengerResponse addPassenger(Long tripId, Long bookingId) {
        Trip trip = findTrip(tripId);
        requireVehicle(trip);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getTrip().getId().equals(tripId)) {
            throw new BusinessException("Booking does not belong to this trip");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Only confirmed bookings can be added as passengers");
        }

        return TripPassengerResponse.from(ensurePassenger(trip, booking));
    }

    @Transactional
    public TripPassengerResponse confirmPassenger(Long tripId, Long passengerId) {
        TripPassenger passenger = findPassenger(tripId, passengerId);
        passenger.transitionTo(PassengerStatus.CONFIRMED);
        passenger.setConfirmedAt(Instant.now());
        return TripPassengerResponse.from(passengerRepository.save(passenger));
    }

    @Transactional
    public TripPassengerResponse boardPassenger(Long tripId, Long passengerId) {
        TripPassenger passenger = findPassenger(tripId, passengerId);
        passenger.transitionTo(PassengerStatus.BOARDED);
        passenger.setBoardedAt(Instant.now());
        return TripPassengerResponse.from(passengerRepository.save(passenger));
    }

    @Transactional
    public TripPassengerResponse removePassenger(Long tripId, Long passengerId) {
        TripPassenger passenger = findPassenger(tripId, passengerId);
        passenger.transitionTo(PassengerStatus.CANCELLED);
        passenger.setSeatNumber(null);
        return TripPassengerResponse.from(passengerRepository.save(passenger));
    }

    /**
     * Reconciles the manifest with the trip's confirmed bookings: adds missing
     * passengers (up to capacity) and cancels entries whose booking is no longer
     * confirmed. Idempotent, so it can run on every read.
     */
    private void syncFromBookings(Trip trip) {
        if (trip.getVehicle() == null) {
            return;
        }
        int capacity = trip.getVehicle().getCapacity();
        List<Booking> confirmedBookings = bookingRepository
                .findByTripIdAndStatus(trip.getId(), BookingStatus.CONFIRMED);
        Set<Long> confirmedBookingIds = confirmedBookings.stream()
                .map(Booking::getId)
                .collect(Collectors.toSet());

        List<TripPassenger> active = passengerRepository
                .findByTripIdAndStatusNot(trip.getId(), PassengerStatus.CANCELLED);
        for (TripPassenger passenger : active) {
            boolean bookingStillConfirmed = confirmedBookingIds.contains(passenger.getBooking().getId());
            if (!bookingStillConfirmed && passenger.getStatus().canTransitionTo(PassengerStatus.CANCELLED)) {
                passenger.setStatus(PassengerStatus.CANCELLED);
                passenger.setSeatNumber(null);
            }
        }

        List<TripPassenger> activeAfterCancel = passengerRepository
                .findByTripIdAndStatusNot(trip.getId(), PassengerStatus.CANCELLED);
        Set<Long> manifestedBookingIds = activeAfterCancel.stream()
                .map(passenger -> passenger.getBooking().getId())
                .collect(Collectors.toSet());
        int activeCount = activeAfterCancel.size();

        for (Booking booking : confirmedBookings) {
            if (manifestedBookingIds.contains(booking.getId())) {
                continue;
            }
            if (activeCount >= capacity) {
                break; // trip already full; extra bookings stay unmanifested
            }
            ensurePassenger(trip, booking);
            activeCount++;
        }
    }

    private TripPassenger ensurePassenger(Trip trip, Booking booking) {
        int capacity = requireVehicle(trip);
        TripPassenger passenger = passengerRepository
                .findByTripIdAndEmployeeId(trip.getId(), booking.getEmployee().getId())
                .orElse(null);

        if (passenger != null && passenger.getStatus() != PassengerStatus.CANCELLED) {
            return passenger;
        }

        int seat = nextAvailableSeat(trip.getId(), capacity);
        if (passenger == null) {
            passenger = new TripPassenger();
            passenger.setTrip(trip);
            passenger.setEmployee(booking.getEmployee());
        }
        passenger.setBooking(booking);
        passenger.setSeatNumber(seat);
        passenger.setStatus(PassengerStatus.EXPECTED);
        passenger.setConfirmedAt(null);
        passenger.setBoardedAt(null);

        return passengerRepository.save(passenger);
    }

    private int nextAvailableSeat(Long tripId, int capacity) {
        Set<Integer> usedSeats = passengerRepository
                .findByTripIdAndStatusNot(tripId, PassengerStatus.CANCELLED).stream()
                .map(TripPassenger::getSeatNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (int seat = 1; seat <= capacity; seat++) {
            if (!usedSeats.contains(seat)) {
                return seat;
            }
        }
        throw new BusinessException("No seats available on this trip");
    }

    private int requireVehicle(Trip trip) {
        if (trip.getVehicle() == null) {
            throw new BusinessException("Trip has no assigned vehicle");
        }
        return trip.getVehicle().getCapacity();
    }

    private Trip findTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
    }

    private TripPassenger findPassenger(Long tripId, Long passengerId) {
        return passengerRepository.findByIdAndTripId(passengerId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger", passengerId));
    }
}
