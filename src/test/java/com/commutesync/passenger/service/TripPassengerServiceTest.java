package com.commutesync.passenger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commutesync.booking.domain.Booking;
import com.commutesync.booking.domain.BookingStatus;
import com.commutesync.booking.repository.BookingRepository;
import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.employee.domain.Employee;
import com.commutesync.employee.domain.EmployeeStatus;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.domain.VehicleStatus;
import com.commutesync.fleet.domain.VehicleType;
import com.commutesync.passenger.domain.PassengerStatus;
import com.commutesync.passenger.domain.TripPassenger;
import com.commutesync.passenger.dto.PassengerSummaryResponse;
import com.commutesync.passenger.dto.TripPassengerResponse;
import com.commutesync.passenger.repository.TripPassengerRepository;
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.domain.TripStatus;
import com.commutesync.trip.repository.TripRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripPassengerServiceTest {

    @Mock
    private TripPassengerRepository passengerRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private BookingRepository bookingRepository;

    private TripPassengerService passengerService;

    private final List<TripPassenger> store = new ArrayList<>();
    private final AtomicLong idSequence = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        passengerService = new TripPassengerService(passengerRepository, tripRepository, bookingRepository);
        store.clear();
    }

    @Test
    void getPassengersSyncsFromConfirmedBookingsAndAssignsSeats() {
        Trip trip = trip(1L, vehicle(3L, 2));
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(bookingRepository.findByTripIdAndStatus(1L, BookingStatus.CONFIRMED))
                .thenReturn(List.of(booking(100L, trip, employee(10L)), booking(101L, trip, employee(11L))));
        stubStore();

        List<TripPassengerResponse> result = passengerService.getPassengers(1L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TripPassengerResponse::seatNumber).containsExactly(1, 2);
        assertThat(result).extracting(TripPassengerResponse::status)
                .containsOnly(PassengerStatus.EXPECTED);
    }

    @Test
    void getPassengersCancelsEntriesWhoseBookingIsNoLongerConfirmed() {
        Trip trip = trip(1L, vehicle(3L, 2));
        Booking booking = booking(100L, trip, employee(10L));
        TripPassenger stale = passenger(50L, trip, booking, employee(10L), 1, PassengerStatus.EXPECTED);
        store.add(stale);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(bookingRepository.findByTripIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(List.of());
        stubStore();

        List<TripPassengerResponse> result = passengerService.getPassengers(1L);

        assertThat(result).isEmpty();
        assertThat(stale.getStatus()).isEqualTo(PassengerStatus.CANCELLED);
        assertThat(stale.getSeatNumber()).isNull();
    }

    @Test
    void addPassengerCreatesManifestEntry() {
        Trip trip = trip(1L, vehicle(3L, 2));
        Booking booking = booking(100L, trip, employee(10L));
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        stubStore();

        TripPassengerResponse response = passengerService.addPassenger(1L, 100L);

        assertThat(response.status()).isEqualTo(PassengerStatus.EXPECTED);
        assertThat(response.seatNumber()).isEqualTo(1);
        assertThat(response.bookingId()).isEqualTo(100L);
        assertThat(store).hasSize(1);
    }

    @Test
    void addPassengerRejectsUnconfirmedBooking() {
        Trip trip = trip(1L, vehicle(3L, 2));
        Booking booking = booking(100L, trip, employee(10L));
        booking.setStatus(BookingStatus.CANCELLED);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> passengerService.addPassenger(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("confirmed bookings");

        verify(passengerRepository, never()).save(any(TripPassenger.class));
    }

    @Test
    void addPassengerRejectsBookingFromAnotherTrip() {
        Trip trip = trip(1L, vehicle(3L, 2));
        Booking booking = booking(100L, trip(2L, vehicle(3L, 2)), employee(10L));
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> passengerService.addPassenger(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong to this trip");
    }

    @Test
    void addPassengerRejectsWhenTripHasNoVehicle() {
        Trip trip = trip(1L, null);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> passengerService.addPassenger(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no assigned vehicle");
    }

    @Test
    void addPassengerRejectsWhenFull() {
        Trip trip = trip(1L, vehicle(3L, 1));
        Booking existing = booking(99L, trip, employee(9L));
        store.add(passenger(50L, trip, existing, employee(9L), 1, PassengerStatus.EXPECTED));
        Booking booking = booking(100L, trip, employee(10L));
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        stubStore();

        assertThatThrownBy(() -> passengerService.addPassenger(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No seats available");
    }

    @Test
    void addPassengerReactivatesCancelledEntry() {
        Trip trip = trip(1L, vehicle(3L, 2));
        Booking booking = booking(100L, trip, employee(10L));
        TripPassenger cancelled = passenger(50L, trip, booking, employee(10L), null, PassengerStatus.CANCELLED);
        store.add(cancelled);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        stubStore();

        TripPassengerResponse response = passengerService.addPassenger(1L, 100L);

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.status()).isEqualTo(PassengerStatus.EXPECTED);
        assertThat(response.seatNumber()).isEqualTo(1);
        assertThat(store).hasSize(1);
    }

    @Test
    void confirmMovesExpectedToConfirmed() {
        TripPassenger passenger = passenger(50L, trip(1L, vehicle(3L, 2)),
                booking(100L, trip(1L, vehicle(3L, 2)), employee(10L)), employee(10L), 1, PassengerStatus.EXPECTED);
        when(passengerRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(passenger));
        when(passengerRepository.save(any(TripPassenger.class))).thenAnswer(inv -> inv.getArgument(0));

        TripPassengerResponse response = passengerService.confirmPassenger(1L, 50L);

        assertThat(response.status()).isEqualTo(PassengerStatus.CONFIRMED);
        assertThat(response.confirmedAt()).isNotNull();
    }

    @Test
    void confirmRejectsCancelledPassenger() {
        TripPassenger passenger = passenger(50L, trip(1L, vehicle(3L, 2)),
                booking(100L, trip(1L, vehicle(3L, 2)), employee(10L)), employee(10L), null, PassengerStatus.CANCELLED);
        when(passengerRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(passenger));

        assertThatThrownBy(() -> passengerService.confirmPassenger(1L, 50L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid passenger state transition");
    }

    @Test
    void boardMovesConfirmedToBoarded() {
        TripPassenger passenger = passenger(50L, trip(1L, vehicle(3L, 2)),
                booking(100L, trip(1L, vehicle(3L, 2)), employee(10L)), employee(10L), 1, PassengerStatus.CONFIRMED);
        when(passengerRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(passenger));
        when(passengerRepository.save(any(TripPassenger.class))).thenAnswer(inv -> inv.getArgument(0));

        TripPassengerResponse response = passengerService.boardPassenger(1L, 50L);

        assertThat(response.status()).isEqualTo(PassengerStatus.BOARDED);
        assertThat(response.boardedAt()).isNotNull();
    }

    @Test
    void boardRejectsExpectedPassenger() {
        TripPassenger passenger = passenger(50L, trip(1L, vehicle(3L, 2)),
                booking(100L, trip(1L, vehicle(3L, 2)), employee(10L)), employee(10L), 1, PassengerStatus.EXPECTED);
        when(passengerRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(passenger));

        assertThatThrownBy(() -> passengerService.boardPassenger(1L, 50L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void removeCancelsPassengerAndFreesSeat() {
        TripPassenger passenger = passenger(50L, trip(1L, vehicle(3L, 2)),
                booking(100L, trip(1L, vehicle(3L, 2)), employee(10L)), employee(10L), 2, PassengerStatus.EXPECTED);
        when(passengerRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(passenger));
        when(passengerRepository.save(any(TripPassenger.class))).thenAnswer(inv -> inv.getArgument(0));

        TripPassengerResponse response = passengerService.removePassenger(1L, 50L);

        assertThat(response.status()).isEqualTo(PassengerStatus.CANCELLED);
        assertThat(response.seatNumber()).isNull();
    }

    @Test
    void removeRejectsBoardedPassenger() {
        TripPassenger passenger = passenger(50L, trip(1L, vehicle(3L, 2)),
                booking(100L, trip(1L, vehicle(3L, 2)), employee(10L)), employee(10L), 2, PassengerStatus.BOARDED);
        when(passengerRepository.findByIdAndTripId(50L, 1L)).thenReturn(Optional.of(passenger));

        assertThatThrownBy(() -> passengerService.removePassenger(1L, 50L))
                .isInstanceOf(BusinessException.class);

        verify(passengerRepository, never()).save(any(TripPassenger.class));
    }

    @Test
    void summaryReportsCountsAndSeatAvailability() {
        Trip trip = trip(1L, vehicle(3L, 3));
        Booking b1 = booking(100L, trip, employee(10L));
        Booking b2 = booking(101L, trip, employee(11L));
        store.add(passenger(50L, trip, b1, employee(10L), 1, PassengerStatus.CONFIRMED));
        store.add(passenger(51L, trip, b2, employee(11L), 2, PassengerStatus.BOARDED));
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(bookingRepository.findByTripIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(List.of(b1, b2));
        stubStore();

        PassengerSummaryResponse summary = passengerService.getSummary(1L);

        assertThat(summary.vehicleCapacity()).isEqualTo(3);
        assertThat(summary.activePassengers()).isEqualTo(2);
        assertThat(summary.confirmedPassengers()).isEqualTo(1);
        assertThat(summary.boardedPassengers()).isEqualTo(1);
        assertThat(summary.availableSeats()).isEqualTo(1);
    }

    @Test
    void getPassengersThrowsWhenTripMissing() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerService.getPassengers(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void stubStore() {
        lenient().when(passengerRepository.save(any(TripPassenger.class))).thenAnswer(invocation -> {
            TripPassenger passenger = invocation.getArgument(0);
            if (passenger.getId() == null) {
                passenger.setId(idSequence.getAndIncrement());
                store.add(passenger);
            }
            return passenger;
        });
        lenient().when(passengerRepository.findByTripIdAndStatusNot(eq(1L), eq(PassengerStatus.CANCELLED)))
                .thenAnswer(invocation -> store.stream()
                        .filter(passenger -> passenger.getStatus().isActive())
                        .toList());
        lenient().when(passengerRepository.findByTripIdAndStatusNotOrderBySeatNumberAsc(
                        eq(1L), eq(PassengerStatus.CANCELLED)))
                .thenAnswer(invocation -> store.stream()
                        .filter(passenger -> passenger.getStatus().isActive())
                        .sorted(Comparator.comparing(TripPassenger::getSeatNumber,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList());
        lenient().when(passengerRepository.findByTripIdAndEmployeeId(eq(1L), anyLong()))
                .thenAnswer(invocation -> {
                    Long employeeId = invocation.getArgument(1);
                    return store.stream()
                            .filter(passenger -> passenger.getEmployee().getId().equals(employeeId))
                            .findFirst();
                });
    }

    private Trip trip(Long id, Vehicle vehicle) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setServiceDate(LocalDate.now().plusDays(1));
        trip.setDepartureTime(LocalTime.of(9, 0));
        trip.setVehicle(vehicle);
        trip.setStatus(TripStatus.ASSIGNED);
        return trip;
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

    private Employee employee(Long id) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setEmployeeCode("EMP00" + id);
        employee.setFullName("Employee " + id);
        employee.setEmail("employee" + id + "@example.com");
        employee.setPhone("+919876543210");
        employee.setPickupAddress("Address " + id);
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }

    private Booking booking(Long id, Trip trip, Employee employee) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setTrip(trip);
        booking.setEmployee(employee);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookedAt(java.time.Instant.now());
        return booking;
    }

    private TripPassenger passenger(Long id, Trip trip, Booking booking, Employee employee,
                                    Integer seatNumber, PassengerStatus status) {
        TripPassenger passenger = new TripPassenger();
        passenger.setId(id);
        passenger.setTrip(trip);
        passenger.setBooking(booking);
        passenger.setEmployee(employee);
        passenger.setSeatNumber(seatNumber);
        passenger.setStatus(status);
        return passenger;
    }
}
