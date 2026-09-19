package com.commutesync.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.commutesync.booking.domain.Booking;
import com.commutesync.booking.domain.BookingStatus;
import com.commutesync.booking.repository.BookingRepository;
import com.commutesync.employee.domain.Employee;
import com.commutesync.employee.domain.EmployeeStatus;
import com.commutesync.employee.repository.EmployeeRepository;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.domain.VehicleStatus;
import com.commutesync.fleet.domain.VehicleType;
import com.commutesync.fleet.repository.VehicleRepository;
import com.commutesync.route.domain.Route;
import com.commutesync.route.domain.RouteStatus;
import com.commutesync.route.repository.RouteRepository;
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.domain.TripStatus;
import com.commutesync.trip.repository.TripRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class BookingRepositoryIT {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void availableForBookingReturnsTripWithFreeSeats() {
        Trip trip = persistTrip(2);

        Page<Trip> available = findAvailable();

        assertThat(available.getContent()).extracting(Trip::getId).contains(trip.getId());
    }

    @Test
    void availableForBookingExcludesFullTrips() {
        Trip trip = persistTrip(1);
        persistBooking(trip, BookingStatus.CONFIRMED, 1);

        Page<Trip> available = findAvailable();

        assertThat(available.getContent()).extracting(Trip::getId).doesNotContain(trip.getId());
    }

    @Test
    void countByTripIdAndStatusCountsOnlyMatchingStatus() {
        Trip trip = persistTrip(3);
        persistBooking(trip, BookingStatus.CONFIRMED, 1);
        persistBooking(trip, BookingStatus.CANCELLED, null);

        assertThat(bookingRepository.countByTripIdAndStatus(trip.getId(), BookingStatus.CONFIRMED))
                .isEqualTo(1);
        assertThat(bookingRepository.countByTripIdAndStatus(trip.getId(), BookingStatus.CANCELLED))
                .isEqualTo(1);
    }

    private Page<Trip> findAvailable() {
        return bookingRepository.findAvailableForBooking(
                EnumSet.of(TripStatus.SCHEDULED, TripStatus.ASSIGNED),
                BookingStatus.CONFIRMED, null, null, PageRequest.of(0, 20));
    }

    private Trip persistTrip(int vehicleCapacity) {
        Route route = new Route();
        route.setRouteCode("R-" + UUID.randomUUID().toString().substring(0, 8));
        route.setName("IT Route");
        route.setSource("Hostel");
        route.setDestination("Office");
        route.setStatus(RouteStatus.ACTIVE);
        route = routeRepository.save(route);

        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationNumber("PB-" + UUID.randomUUID().toString().substring(0, 8));
        vehicle.setType(VehicleType.VAN);
        vehicle.setCapacity(vehicleCapacity);
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle = vehicleRepository.save(vehicle);

        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setVehicle(vehicle);
        trip.setServiceDate(LocalDate.now().plusDays(1));
        trip.setDepartureTime(LocalTime.of(9, 0));
        trip.setStatus(TripStatus.ASSIGNED);
        return tripRepository.save(trip);
    }

    private void persistBooking(Trip trip, BookingStatus status, Integer seatNumber) {
        Employee employee = new Employee();
        employee.setEmployeeCode("EMP-" + UUID.randomUUID().toString().substring(0, 8));
        employee.setFullName("IT Employee");
        employee.setEmail("emp-" + UUID.randomUUID() + "@example.com");
        employee.setPhone("+919876543210");
        employee.setPickupAddress("IT Address");
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee = employeeRepository.save(employee);

        Booking booking = new Booking();
        booking.setTrip(trip);
        booking.setEmployee(employee);
        booking.setStatus(status);
        booking.setSeatNumber(seatNumber);
        booking.setBookedAt(Instant.now());
        bookingRepository.save(booking);
    }
}
