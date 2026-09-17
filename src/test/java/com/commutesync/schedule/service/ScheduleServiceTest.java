package com.commutesync.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.DuplicateResourceException;
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
import com.commutesync.schedule.domain.Shift;
import com.commutesync.schedule.domain.ShiftStatus;
import com.commutesync.schedule.dto.CreateScheduleRequest;
import com.commutesync.schedule.dto.ScheduleResponse;
import com.commutesync.schedule.dto.UpdateScheduleRequest;
import com.commutesync.schedule.repository.ScheduleRepository;
import com.commutesync.schedule.repository.ShiftRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    private static final LocalDate SERVICE_DATE = LocalDate.now().plusDays(1);
    private static final LocalTime DEPARTURE = LocalTime.of(9, 0);

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void createSavesSchedule() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift(1L, ShiftStatus.ACTIVE)));
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route(2L, RouteStatus.ACTIVE)));
        when(scheduleRepository.existsByServiceDateAndShiftIdAndRouteIdAndDepartureTime(
                SERVICE_DATE, 1L, 2L, DEPARTURE)).thenReturn(false);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ScheduleResponse response = scheduleService.create(new CreateScheduleRequest(
                1L, 2L, SERVICE_DATE, DEPARTURE, DEPARTURE.plusMinutes(45), null, null, null));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(ScheduleStatus.ACTIVE);
        assertThat(response.shift().id()).isEqualTo(1L);
        assertThat(response.route().id()).isEqualTo(2L);
        assertThat(response.vehicle()).isNull();
        assertThat(response.driver()).isNull();
    }

    @Test
    void createRejectsInactiveShift() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift(1L, ShiftStatus.INACTIVE)));

        assertThatThrownBy(() -> scheduleService.create(new CreateScheduleRequest(
                1L, 2L, SERVICE_DATE, DEPARTURE, null, null, null, null)))
                .isInstanceOf(BusinessException.class);

        verify(routeRepository, never()).findById(any());
    }

    @Test
    void createRejectsInactiveRoute() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift(1L, ShiftStatus.ACTIVE)));
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route(2L, RouteStatus.INACTIVE)));

        assertThatThrownBy(() -> scheduleService.create(new CreateScheduleRequest(
                1L, 2L, SERVICE_DATE, DEPARTURE, null, null, null, null)))
                .isInstanceOf(BusinessException.class);

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void createRejectsUnavailableVehicle() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift(1L, ShiftStatus.ACTIVE)));
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route(2L, RouteStatus.ACTIVE)));
        when(vehicleRepository.findById(3L)).thenReturn(Optional.of(vehicle(3L, VehicleStatus.MAINTENANCE)));

        assertThatThrownBy(() -> scheduleService.create(new CreateScheduleRequest(
                1L, 2L, SERVICE_DATE, DEPARTURE, null, 3L, null, null)))
                .isInstanceOf(BusinessException.class);

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void createRejectsUnavailableDriver() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift(1L, ShiftStatus.ACTIVE)));
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route(2L, RouteStatus.ACTIVE)));
        when(driverRepository.findById(4L)).thenReturn(Optional.of(
                driver(4L, DriverStatus.INACTIVE, true, LocalDate.now().plusYears(1))));

        assertThatThrownBy(() -> scheduleService.create(new CreateScheduleRequest(
                1L, 2L, SERVICE_DATE, DEPARTURE, null, null, 4L, null)))
                .isInstanceOf(BusinessException.class);

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void createRejectsDuplicateSlot() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift(1L, ShiftStatus.ACTIVE)));
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route(2L, RouteStatus.ACTIVE)));
        when(scheduleRepository.existsByServiceDateAndShiftIdAndRouteIdAndDepartureTime(
                SERVICE_DATE, 1L, 2L, DEPARTURE)).thenReturn(true);

        assertThatThrownBy(() -> scheduleService.create(new CreateScheduleRequest(
                1L, 2L, SERVICE_DATE, DEPARTURE, null, null, null, null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void createRejectsArrivalEqualToDeparture() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift(1L, ShiftStatus.ACTIVE)));
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route(2L, RouteStatus.ACTIVE)));

        assertThatThrownBy(() -> scheduleService.create(new CreateScheduleRequest(
                1L, 2L, SERVICE_DATE, DEPARTURE, DEPARTURE, null, null, null)))
                .isInstanceOf(BusinessException.class);

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(scheduleRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppliesChanges() {
        Schedule existing = schedule(1L, shift(1L, ShiftStatus.ACTIVE), route(2L, RouteStatus.ACTIVE),
                SERVICE_DATE, DEPARTURE);
        when(scheduleRepository.findWithDetailsById(1L)).thenReturn(Optional.of(existing));
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift(1L, ShiftStatus.ACTIVE)));
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route(2L, RouteStatus.ACTIVE)));
        LocalDate newDate = SERVICE_DATE.plusDays(1);
        LocalTime newDeparture = LocalTime.of(10, 0);
        when(scheduleRepository.existsByServiceDateAndShiftIdAndRouteIdAndDepartureTime(
                newDate, 1L, 2L, newDeparture)).thenReturn(false);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleResponse response = scheduleService.update(1L, new UpdateScheduleRequest(
                1L, 2L, newDate, newDeparture, newDeparture.plusMinutes(30), null, null,
                ScheduleStatus.INACTIVE));

        assertThat(response.serviceDate()).isEqualTo(newDate);
        assertThat(response.departureTime()).isEqualTo(newDeparture);
        assertThat(response.status()).isEqualTo(ScheduleStatus.INACTIVE);
    }

    @Test
    void updateStatusChangesStatus() {
        Schedule existing = schedule(1L, shift(1L, ShiftStatus.ACTIVE), route(2L, RouteStatus.ACTIVE),
                SERVICE_DATE, DEPARTURE);
        when(scheduleRepository.findWithDetailsById(1L)).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleResponse response = scheduleService.updateStatus(1L, ScheduleStatus.INACTIVE);

        assertThat(response.status()).isEqualTo(ScheduleStatus.INACTIVE);
    }

    @Test
    void deleteRemovesSchedule() {
        Schedule existing = schedule(1L, shift(1L, ShiftStatus.ACTIVE), route(2L, RouteStatus.ACTIVE),
                SERVICE_DATE, DEPARTURE);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(existing));

        scheduleService.delete(1L);

        verify(scheduleRepository).delete(existing);
    }

    private Shift shift(Long id, ShiftStatus status) {
        Shift shift = new Shift();
        shift.setId(id);
        shift.setShiftCode("MORNING");
        shift.setName("Morning Shift");
        shift.setStartTime(LocalTime.of(9, 0));
        shift.setEndTime(LocalTime.of(18, 0));
        shift.setStatus(status);
        return shift;
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

    private Vehicle vehicle(Long id, VehicleStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setRegistrationNumber("PB10AB1234");
        vehicle.setType(VehicleType.VAN);
        vehicle.setCapacity(12);
        vehicle.setStatus(status);
        return vehicle;
    }

    private Driver driver(Long id, DriverStatus status, boolean available, LocalDate expiry) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setFullName("Driver " + id);
        driver.setPhone("+919876543210");
        driver.setEmail("driver" + id + "@example.com");
        driver.setLicenseNumber("DL-" + id);
        driver.setLicenseExpiryDate(expiry);
        driver.setStatus(status);
        driver.setAvailable(available);
        return driver;
    }

    private Schedule schedule(Long id, Shift shift, Route route, LocalDate date, LocalTime departure) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setShift(shift);
        schedule.setRoute(route);
        schedule.setServiceDate(date);
        schedule.setDepartureTime(departure);
        schedule.setStatus(ScheduleStatus.ACTIVE);
        return schedule;
    }
}
