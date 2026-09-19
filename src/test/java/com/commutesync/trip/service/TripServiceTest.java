package com.commutesync.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.fleet.domain.Driver;
import com.commutesync.fleet.domain.DriverStatus;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.domain.VehicleStatus;
import com.commutesync.fleet.domain.VehicleType;
import com.commutesync.fleet.repository.DriverRepository;
import com.commutesync.fleet.repository.VehicleRepository;
import com.commutesync.route.domain.Route;
import com.commutesync.route.domain.RouteStatus;
import com.commutesync.route.repository.RouteRepository;
import com.commutesync.schedule.domain.Schedule;
import com.commutesync.schedule.domain.ScheduleStatus;
import com.commutesync.schedule.repository.ScheduleRepository;
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.domain.TripStatus;
import com.commutesync.trip.dto.AssignTripRequest;
import com.commutesync.trip.dto.CreateTripRequest;
import com.commutesync.trip.dto.TripResponse;
import com.commutesync.trip.repository.TripRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    private static final LocalDate SERVICE_DATE = LocalDate.now().plusDays(1);
    private static final LocalTime DEPARTURE = LocalTime.of(9, 0);

    @Mock
    private TripRepository tripRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private TripService tripService;

    @Test
    void createStandaloneTripIsScheduled() {
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route(2L, RouteStatus.ACTIVE)));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TripResponse response = tripService.create(new CreateTripRequest(
                null, 2L, SERVICE_DATE, DEPARTURE, null, null, null));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(TripStatus.SCHEDULED);
        assertThat(response.driver()).isNull();
        assertThat(response.vehicle()).isNull();
        assertThat(response.route().id()).isEqualTo(2L);
    }

    @Test
    void createWithDriverAndVehicleIsAssigned() {
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route(2L, RouteStatus.ACTIVE)));
        when(driverRepository.findById(4L)).thenReturn(Optional.of(driver(4L, DriverStatus.ACTIVE, true)));
        when(vehicleRepository.findById(3L)).thenReturn(Optional.of(vehicle(3L, VehicleStatus.AVAILABLE)));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.create(new CreateTripRequest(
                null, 2L, SERVICE_DATE, DEPARTURE, null, 4L, 3L));

        assertThat(response.status()).isEqualTo(TripStatus.ASSIGNED);
        assertThat(response.driver().id()).isEqualTo(4L);
        assertThat(response.vehicle().id()).isEqualTo(3L);
    }

    @Test
    void createFromScheduleInheritsDetails() {
        Schedule schedule = schedule(5L, ScheduleStatus.ACTIVE,
                route(2L, RouteStatus.ACTIVE),
                driver(4L, DriverStatus.ACTIVE, true),
                vehicle(3L, VehicleStatus.AVAILABLE));
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(schedule));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.create(new CreateTripRequest(
                5L, null, null, null, null, null, null));

        assertThat(response.scheduleId()).isEqualTo(5L);
        assertThat(response.serviceDate()).isEqualTo(SERVICE_DATE);
        assertThat(response.route().id()).isEqualTo(2L);
        assertThat(response.status()).isEqualTo(TripStatus.ASSIGNED);
    }

    @Test
    void createRejectsInactiveRoute() {
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route(2L, RouteStatus.INACTIVE)));

        assertThatThrownBy(() -> tripService.create(new CreateTripRequest(
                null, 2L, SERVICE_DATE, DEPARTURE, null, null, null)))
                .isInstanceOf(BusinessException.class);

        verify(tripRepository, never()).save(any(Trip.class));
    }

    @Test
    void createRejectsInactiveSchedule() {
        when(scheduleRepository.findById(5L)).thenReturn(Optional.of(
                schedule(5L, ScheduleStatus.INACTIVE, route(2L, RouteStatus.ACTIVE), null, null)));

        assertThatThrownBy(() -> tripService.create(new CreateTripRequest(
                5L, null, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class);

        verify(tripRepository, never()).save(any(Trip.class));
    }

    @Test
    void createRejectsUnavailableVehicle() {
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route(2L, RouteStatus.ACTIVE)));
        when(driverRepository.findById(4L)).thenReturn(Optional.of(driver(4L, DriverStatus.ACTIVE, true)));
        when(vehicleRepository.findById(3L)).thenReturn(Optional.of(vehicle(3L, VehicleStatus.MAINTENANCE)));

        assertThatThrownBy(() -> tripService.create(new CreateTripRequest(
                null, 2L, SERVICE_DATE, DEPARTURE, null, 4L, 3L)))
                .isInstanceOf(BusinessException.class);

        verify(tripRepository, never()).save(any(Trip.class));
    }

    @Test
    void assignMovesScheduledToAssigned() {
        Trip trip = trip(1L, TripStatus.SCHEDULED, null, null);
        when(tripRepository.findWithDetailsById(1L)).thenReturn(Optional.of(trip));
        when(driverRepository.findById(4L)).thenReturn(Optional.of(driver(4L, DriverStatus.ACTIVE, true)));
        when(vehicleRepository.findById(3L)).thenReturn(Optional.of(vehicle(3L, VehicleStatus.AVAILABLE)));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.assign(1L, new AssignTripRequest(4L, 3L));

        assertThat(response.status()).isEqualTo(TripStatus.ASSIGNED);
        assertThat(response.driver().id()).isEqualTo(4L);
    }

    @Test
    void assignRejectsAfterStarted() {
        Trip trip = trip(1L, TripStatus.STARTED,
                driver(4L, DriverStatus.ACTIVE, true), vehicle(3L, VehicleStatus.AVAILABLE));
        when(tripRepository.findWithDetailsById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.assign(1L, new AssignTripRequest(4L, 3L)))
                .isInstanceOf(BusinessException.class);

        verify(driverRepository, never()).findById(any());
    }

    @Test
    void startMovesAssignedToStarted() {
        Trip trip = trip(1L, TripStatus.ASSIGNED,
                driver(4L, DriverStatus.ACTIVE, true), vehicle(3L, VehicleStatus.AVAILABLE));
        when(tripRepository.findWithDetailsById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.start(1L);

        assertThat(response.status()).isEqualTo(TripStatus.STARTED);
        assertThat(response.startedAt()).isNotNull();
    }

    @Test
    void startRejectsInvalidTransitionFromScheduled() {
        Trip trip = trip(1L, TripStatus.SCHEDULED,
                driver(4L, DriverStatus.ACTIVE, true), vehicle(3L, VehicleStatus.AVAILABLE));
        when(tripRepository.findWithDetailsById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.start(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid trip state transition");
    }

    @Test
    void startRejectsWithoutDriverOrVehicle() {
        Trip trip = trip(1L, TripStatus.ASSIGNED, null, null);
        when(tripRepository.findWithDetailsById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.start(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("driver and a vehicle");

        verify(tripRepository, never()).save(any(Trip.class));
    }

    @Test
    void markInProgressMovesStartedToInProgress() {
        Trip trip = trip(1L, TripStatus.STARTED,
                driver(4L, DriverStatus.ACTIVE, true), vehicle(3L, VehicleStatus.AVAILABLE));
        when(tripRepository.findWithDetailsById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.markInProgress(1L);

        assertThat(response.status()).isEqualTo(TripStatus.IN_PROGRESS);
    }

    @Test
    void markInProgressRejectsFromAssigned() {
        Trip trip = trip(1L, TripStatus.ASSIGNED,
                driver(4L, DriverStatus.ACTIVE, true), vehicle(3L, VehicleStatus.AVAILABLE));
        when(tripRepository.findWithDetailsById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.markInProgress(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void completeMovesInProgressToCompleted() {
        Trip trip = trip(1L, TripStatus.IN_PROGRESS,
                driver(4L, DriverStatus.ACTIVE, true), vehicle(3L, VehicleStatus.AVAILABLE));
        when(tripRepository.findWithDetailsById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.complete(1L);

        assertThat(response.status()).isEqualTo(TripStatus.COMPLETED);
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void completeRejectsFromStarted() {
        Trip trip = trip(1L, TripStatus.STARTED,
                driver(4L, DriverStatus.ACTIVE, true), vehicle(3L, VehicleStatus.AVAILABLE));
        when(tripRepository.findWithDetailsById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.complete(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancelMovesAssignedToCancelled() {
        Trip trip = trip(1L, TripStatus.ASSIGNED,
                driver(4L, DriverStatus.ACTIVE, true), vehicle(3L, VehicleStatus.AVAILABLE));
        when(tripRepository.findWithDetailsById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.cancel(1L, "Vehicle breakdown");

        assertThat(response.status()).isEqualTo(TripStatus.CANCELLED);
        assertThat(response.cancelledAt()).isNotNull();
        assertThat(response.cancellationReason()).isEqualTo("Vehicle breakdown");
    }

    @Test
    void cancelRejectsFromCompleted() {
        Trip trip = trip(1L, TripStatus.COMPLETED,
                driver(4L, DriverStatus.ACTIVE, true), vehicle(3L, VehicleStatus.AVAILABLE));
        when(tripRepository.findWithDetailsById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.cancel(1L, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void changeStatusDelegatesToStart() {
        Trip trip = trip(1L, TripStatus.ASSIGNED,
                driver(4L, DriverStatus.ACTIVE, true), vehicle(3L, VehicleStatus.AVAILABLE));
        when(tripRepository.findWithDetailsById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.changeStatus(1L, TripStatus.STARTED, null);

        assertThat(response.status()).isEqualTo(TripStatus.STARTED);
    }

    @Test
    void changeStatusRejectsAssignedTarget() {
        assertThatThrownBy(() -> tripService.changeStatus(1L, TripStatus.ASSIGNED, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("assignment");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(tripRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Trip trip(Long id, TripStatus status, Driver driver, Vehicle vehicle) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setRoute(route(2L, RouteStatus.ACTIVE));
        trip.setServiceDate(SERVICE_DATE);
        trip.setDepartureTime(DEPARTURE);
        trip.setDriver(driver);
        trip.setVehicle(vehicle);
        trip.setStatus(status);
        return trip;
    }

    private Route route(Long id, RouteStatus status) {
        Route route = new Route();
        route.setId(id);
        route.setRouteCode("R-01");
        route.setName("Morning Route");
        route.setSource("Hostel");
        route.setDestination("Office");
        route.setStatus(status);
        return route;
    }

    private Driver driver(Long id, DriverStatus status, boolean available) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setFullName("Driver " + id);
        driver.setPhone("+919876543210");
        driver.setEmail("driver" + id + "@example.com");
        driver.setLicenseNumber("DL-" + id);
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(1));
        driver.setStatus(status);
        driver.setAvailable(available);
        return driver;
    }

    private Vehicle vehicle(Long id, VehicleStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setRegistrationNumber("PB10AB1234");
        vehicle.setType(VehicleType.VAN);
        vehicle.setCapacity(12);
        vehicle.setStatus(status);
        return vehicle;
    }

    private Schedule schedule(Long id, ScheduleStatus status, Route route, Driver driver, Vehicle vehicle) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setRoute(route);
        schedule.setServiceDate(SERVICE_DATE);
        schedule.setDepartureTime(DEPARTURE);
        schedule.setDriver(driver);
        schedule.setVehicle(vehicle);
        schedule.setStatus(status);
        return schedule;
    }
}
