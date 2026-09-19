package com.commutesync.booking.service;

import com.commutesync.booking.domain.Booking;
import com.commutesync.booking.domain.BookingStatus;
import com.commutesync.booking.dto.AvailableTripResponse;
import com.commutesync.booking.dto.BookingResponse;
import com.commutesync.booking.event.BookingCancelledEvent;
import com.commutesync.booking.event.BookingConfirmedEvent;
import com.commutesync.booking.repository.BookingRepository;
import com.commutesync.booking.repository.BookingRepository.TripSeatCount;
import com.commutesync.common.api.PageResponse;
import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.DuplicateResourceException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.employee.domain.Employee;
import com.commutesync.employee.repository.EmployeeRepository;
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.domain.TripStatus;
import com.commutesync.trip.repository.TripRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private static final Collection<TripStatus> BOOKABLE_STATUSES =
            EnumSet.of(TripStatus.SCHEDULED, TripStatus.ASSIGNED);

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final EmployeeRepository employeeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(BookingRepository bookingRepository,
                          TripRepository tripRepository,
                          EmployeeRepository employeeRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.tripRepository = tripRepository;
        this.employeeRepository = employeeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public PageResponse<AvailableTripResponse> getAvailableTrips(LocalDate serviceDate, Long routeId,
                                                                 Pageable pageable) {
        Page<Trip> trips = bookingRepository.findAvailableForBooking(
                BOOKABLE_STATUSES, BookingStatus.CONFIRMED, serviceDate, routeId, pageable);

        Map<Long, Long> confirmedByTrip = confirmedCounts(trips.getContent());
        Page<AvailableTripResponse> mapped = trips.map(trip ->
                AvailableTripResponse.of(trip, confirmedByTrip.getOrDefault(trip.getId(), 0L)));
        return PageResponse.from(mapped);
    }

    @Transactional
    public BookingResponse book(Long tripId, String employeeEmail) {
        // Row lock serializes concurrent bookings for this trip (safe for the last seat).
        Trip trip = tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
        Employee employee = requireEmployee(employeeEmail);
        ensureBookable(trip);

        long confirmed = bookingRepository.countByTripIdAndStatus(tripId, BookingStatus.CONFIRMED);
        int capacity = trip.getVehicle().getCapacity();
        if (confirmed >= capacity) {
            throw new BusinessException("No seats available on this trip");
        }

        Booking booking = bookingRepository.findByTripIdAndEmployeeId(tripId, employee.getId())
                .orElseGet(() -> {
                    Booking created = new Booking();
                    created.setTrip(trip);
                    created.setEmployee(employee);
                    return created;
                });

        if (booking.getId() != null && booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new DuplicateResourceException("Employee has already booked this trip");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookedAt(Instant.now());
        booking.setCancelledAt(null);
        booking.setCancellationReason(null);
        booking.setSeatNumber((int) confirmed + 1);

        Booking saved = bookingRepository.save(booking);
        eventPublisher.publishEvent(new BookingConfirmedEvent(
                saved.getId(), trip.getId(), employee.getEmail()));
        return BookingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long id, String requesterEmail, boolean admin) {
        Booking booking = findWithDetails(id);
        ensureOwnerOrAdmin(booking, requesterEmail, admin);
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getMyBookings(String employeeEmail, Pageable pageable) {
        Employee employee = requireEmployee(employeeEmail);
        Page<Booking> page = bookingRepository.findByEmployeeIdWithDetails(employee.getId(), pageable);
        return PageResponse.from(page.map(BookingResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getAllBookings(Long tripId, Long employeeId,
                                                        BookingStatus status, Pageable pageable) {
        Page<Booking> page = bookingRepository.search(tripId, employeeId, status, pageable);
        return PageResponse.from(page.map(BookingResponse::from));
    }

    @Transactional
    public BookingResponse cancel(Long id, String requesterEmail, boolean admin, String reason) {
        Booking booking = findWithDetails(id);
        ensureOwnerOrAdmin(booking, requesterEmail, admin);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Booking is not active");
        }
        TripStatus tripStatus = booking.getTrip().getStatus();
        if (tripStatus != TripStatus.SCHEDULED && tripStatus != TripStatus.ASSIGNED) {
            throw new BusinessException("Booking cannot be cancelled after the trip has started");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        booking.setCancellationReason(trimToNull(reason));
        booking.setSeatNumber(null); // release the seat for the unique constraint

        Booking saved = bookingRepository.save(booking);
        eventPublisher.publishEvent(new BookingCancelledEvent(
                saved.getId(), saved.getTrip().getId(), saved.getEmployee().getEmail()));
        return BookingResponse.from(saved);
    }

    private void ensureBookable(Trip trip) {
        if (trip.getStatus() != TripStatus.SCHEDULED && trip.getStatus() != TripStatus.ASSIGNED) {
            throw new BusinessException("Trip is not open for booking (status: " + trip.getStatus() + ")");
        }
        if (trip.getVehicle() == null) {
            throw new BusinessException("Trip has no assigned vehicle yet");
        }
    }

    private void ensureOwnerOrAdmin(Booking booking, String requesterEmail, boolean admin) {
        if (!admin && !booking.getEmployee().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new AccessDeniedException("You can only access your own bookings");
        }
    }

    private Employee requireEmployee(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee profile for user " + email));
    }

    private Booking findWithDetails(Long id) {
        return bookingRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    private Map<Long, Long> confirmedCounts(List<Trip> trips) {
        if (trips.isEmpty()) {
            return Map.of();
        }
        List<Long> tripIds = trips.stream().map(Trip::getId).toList();
        return bookingRepository.countConfirmedByTripIds(tripIds, BookingStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(TripSeatCount::getTripId, TripSeatCount::getConfirmedBookings));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
