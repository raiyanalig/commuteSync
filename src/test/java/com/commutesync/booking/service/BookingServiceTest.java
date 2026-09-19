package com.commutesync.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commutesync.booking.domain.Booking;
import com.commutesync.booking.domain.BookingStatus;
import com.commutesync.booking.dto.AvailableTripResponse;
import com.commutesync.booking.dto.BookingResponse;
import com.commutesync.booking.repository.BookingRepository;
import com.commutesync.booking.repository.BookingRepository.TripSeatCount;
import com.commutesync.common.api.PageResponse;
import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.DuplicateResourceException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.employee.domain.Employee;
import com.commutesync.employee.domain.EmployeeStatus;
import com.commutesync.employee.repository.EmployeeRepository;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.domain.VehicleStatus;
import com.commutesync.fleet.domain.VehicleType;
import com.commutesync.route.domain.Route;
import com.commutesync.route.domain.RouteStatus;
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.domain.TripStatus;
import com.commutesync.trip.repository.TripRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void bookConfirmsAndAssignsFirstSeat() {
        Trip trip = trip(1L, TripStatus.ASSIGNED, vehicle(3L, 12));
        Employee employee = employee(10L, "raiyan@example.com");
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(employeeRepository.findByEmail("raiyan@example.com")).thenReturn(Optional.of(employee));
        when(bookingRepository.countByTripIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(0L);
        when(bookingRepository.findByTripIdAndEmployeeId(1L, 10L)).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        BookingResponse response = bookingService.book(1L, "raiyan@example.com");

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.seatNumber()).isEqualTo(1);
        assertThat(response.trip().id()).isEqualTo(1L);
        assertThat(response.employee().email()).isEqualTo("raiyan@example.com");
    }

    @Test
    void bookAssignsNextSeat() {
        Trip trip = trip(1L, TripStatus.ASSIGNED, vehicle(3L, 12));
        Employee employee = employee(10L, "raiyan@example.com");
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(employeeRepository.findByEmail("raiyan@example.com")).thenReturn(Optional.of(employee));
        when(bookingRepository.countByTripIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(4L);
        when(bookingRepository.findByTripIdAndEmployeeId(1L, 10L)).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.book(1L, "raiyan@example.com");

        assertThat(response.seatNumber()).isEqualTo(5);
    }

    @Test
    void bookRejectsDuplicateConfirmedBooking() {
        Trip trip = trip(1L, TripStatus.ASSIGNED, vehicle(3L, 12));
        Employee employee = employee(10L, "raiyan@example.com");
        Booking existing = booking(100L, BookingStatus.CONFIRMED, trip, employee, 2);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(employeeRepository.findByEmail("raiyan@example.com")).thenReturn(Optional.of(employee));
        when(bookingRepository.countByTripIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(1L);
        when(bookingRepository.findByTripIdAndEmployeeId(1L, 10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> bookingService.book(1L, "raiyan@example.com"))
                .isInstanceOf(DuplicateResourceException.class);

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void bookReactivatesCancelledBooking() {
        Trip trip = trip(1L, TripStatus.ASSIGNED, vehicle(3L, 12));
        Employee employee = employee(10L, "raiyan@example.com");
        Booking existing = booking(100L, BookingStatus.CANCELLED, trip, employee, null);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(employeeRepository.findByEmail("raiyan@example.com")).thenReturn(Optional.of(employee));
        when(bookingRepository.countByTripIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(2L);
        when(bookingRepository.findByTripIdAndEmployeeId(1L, 10L)).thenReturn(Optional.of(existing));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.book(1L, "raiyan@example.com");

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.seatNumber()).isEqualTo(3);
    }

    @Test
    void bookRejectsWhenNoSeats() {
        Trip trip = trip(1L, TripStatus.ASSIGNED, vehicle(3L, 12));
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(employeeRepository.findByEmail("raiyan@example.com"))
                .thenReturn(Optional.of(employee(10L, "raiyan@example.com")));
        when(bookingRepository.countByTripIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(12L);

        assertThatThrownBy(() -> bookingService.book(1L, "raiyan@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No seats available");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void bookRejectsWhenTripNotOpen() {
        Trip trip = trip(1L, TripStatus.IN_PROGRESS, vehicle(3L, 12));
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(employeeRepository.findByEmail("raiyan@example.com"))
                .thenReturn(Optional.of(employee(10L, "raiyan@example.com")));

        assertThatThrownBy(() -> bookingService.book(1L, "raiyan@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not open for booking");
    }

    @Test
    void bookRejectsWhenTripHasNoVehicle() {
        Trip trip = trip(1L, TripStatus.ASSIGNED, null);
        when(tripRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
        when(employeeRepository.findByEmail("raiyan@example.com"))
                .thenReturn(Optional.of(employee(10L, "raiyan@example.com")));

        assertThatThrownBy(() -> bookingService.book(1L, "raiyan@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no assigned vehicle");
    }

    @Test
    void bookThrowsWhenTripMissing() {
        when(tripRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.book(99L, "raiyan@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void bookThrowsWhenEmployeeProfileMissing() {
        when(tripRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(trip(1L, TripStatus.ASSIGNED, vehicle(3L, 12))));
        when(employeeRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.book(1L, "ghost@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelMarksCancelledAndReleasesSeat() {
        Booking booking = booking(100L, BookingStatus.CONFIRMED,
                trip(1L, TripStatus.ASSIGNED, vehicle(3L, 12)),
                employee(10L, "raiyan@example.com"), 2);
        when(bookingRepository.findWithDetailsById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancel(100L, "raiyan@example.com", false, "Changed plans");

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.seatNumber()).isNull();
        assertThat(response.cancellationReason()).isEqualTo("Changed plans");
        assertThat(response.cancelledAt()).isNotNull();
    }

    @Test
    void cancelRejectsNonOwner() {
        Booking booking = booking(100L, BookingStatus.CONFIRMED,
                trip(1L, TripStatus.ASSIGNED, vehicle(3L, 12)),
                employee(10L, "other@example.com"), 2);
        when(bookingRepository.findWithDetailsById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(100L, "raiyan@example.com", false, null))
                .isInstanceOf(AccessDeniedException.class);

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void cancelAllowsAdminOnOthersBooking() {
        Booking booking = booking(100L, BookingStatus.CONFIRMED,
                trip(1L, TripStatus.ASSIGNED, vehicle(3L, 12)),
                employee(10L, "other@example.com"), 2);
        when(bookingRepository.findWithDetailsById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancel(100L, "admin@example.com", true, null);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancelRejectsInactiveBooking() {
        Booking booking = booking(100L, BookingStatus.CANCELLED,
                trip(1L, TripStatus.ASSIGNED, vehicle(3L, 12)),
                employee(10L, "raiyan@example.com"), null);
        when(bookingRepository.findWithDetailsById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(100L, "raiyan@example.com", false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void cancelRejectsAfterTripStarted() {
        Booking booking = booking(100L, BookingStatus.CONFIRMED,
                trip(1L, TripStatus.STARTED, vehicle(3L, 12)),
                employee(10L, "raiyan@example.com"), 2);
        when(bookingRepository.findWithDetailsById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(100L, "raiyan@example.com", false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("after the trip has started");
    }

    @Test
    void getBookingAllowsOwner() {
        Booking booking = booking(100L, BookingStatus.CONFIRMED,
                trip(1L, TripStatus.ASSIGNED, vehicle(3L, 12)),
                employee(10L, "raiyan@example.com"), 2);
        when(bookingRepository.findWithDetailsById(100L)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.getBooking(100L, "raiyan@example.com", false);

        assertThat(response.id()).isEqualTo(100L);
    }

    @Test
    void getBookingRejectsNonOwner() {
        Booking booking = booking(100L, BookingStatus.CONFIRMED,
                trip(1L, TripStatus.ASSIGNED, vehicle(3L, 12)),
                employee(10L, "other@example.com"), 2);
        when(bookingRepository.findWithDetailsById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getBooking(100L, "raiyan@example.com", false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAvailableTripsComputesAvailableSeats() {
        Trip trip = trip(1L, TripStatus.ASSIGNED, vehicle(3L, 12));
        Page<Trip> page = new PageImpl<>(List.of(trip));
        when(bookingRepository.findAvailableForBooking(
                anyCollection(), eq(BookingStatus.CONFIRMED), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(bookingRepository.countConfirmedByTripIds(anyCollection(), eq(BookingStatus.CONFIRMED)))
                .thenReturn(List.of(new SeatCount(1L, 5L)));

        PageResponse<AvailableTripResponse> response =
                bookingService.getAvailableTrips(null, null, PageRequest.of(0, 10));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).confirmedBookings()).isEqualTo(5);
        assertThat(response.content().get(0).availableSeats()).isEqualTo(7);
    }

    @Test
    void getMyBookingsReturnsHistory() {
        when(employeeRepository.findByEmail("raiyan@example.com"))
                .thenReturn(Optional.of(employee(10L, "raiyan@example.com")));
        Booking booking = booking(100L, BookingStatus.CONFIRMED,
                trip(1L, TripStatus.ASSIGNED, vehicle(3L, 12)),
                employee(10L, "raiyan@example.com"), 2);
        when(bookingRepository.findByEmployeeIdWithDetails(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));

        PageResponse<BookingResponse> response =
                bookingService.getMyBookings("raiyan@example.com", PageRequest.of(0, 10));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    private Trip trip(Long id, TripStatus status, Vehicle vehicle) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setRoute(route(2L));
        trip.setServiceDate(LocalDate.now().plusDays(1));
        trip.setDepartureTime(LocalTime.of(9, 0));
        trip.setVehicle(vehicle);
        trip.setStatus(status);
        return trip;
    }

    private Route route(Long id) {
        Route route = new Route();
        route.setId(id);
        route.setRouteCode("R-01");
        route.setName("Morning Route");
        route.setSource("Hostel");
        route.setDestination("Office");
        route.setStatus(RouteStatus.ACTIVE);
        return route;
    }

    private Vehicle vehicle(Long id, int capacity) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setRegistrationNumber("PB10AB1234");
        vehicle.setType(VehicleType.VAN);
        vehicle.setCapacity(capacity);
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        return vehicle;
    }

    private Employee employee(Long id, String email) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setEmployeeCode("EMP00" + id);
        employee.setFullName("Employee " + id);
        employee.setEmail(email);
        employee.setPhone("+919876543210");
        employee.setPickupAddress("Address " + id);
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }

    private Booking booking(Long id, BookingStatus status, Trip trip, Employee employee, Integer seatNumber) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setTrip(trip);
        booking.setEmployee(employee);
        booking.setStatus(status);
        booking.setSeatNumber(seatNumber);
        booking.setBookedAt(java.time.Instant.now());
        return booking;
    }

    private static final class SeatCount implements TripSeatCount {
        private final Long tripId;
        private final long count;

        private SeatCount(Long tripId, long count) {
            this.tripId = tripId;
            this.count = count;
        }

        @Override
        public Long getTripId() {
            return tripId;
        }

        @Override
        public long getConfirmedBookings() {
            return count;
        }
    }
}
